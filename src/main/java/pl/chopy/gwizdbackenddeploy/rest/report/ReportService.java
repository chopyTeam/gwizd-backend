package pl.chopy.gwizdbackenddeploy.rest.report;

import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.chopy.gwizdbackenddeploy.model.ReportType;
import pl.chopy.gwizdbackenddeploy.model.entity.Animal;
import pl.chopy.gwizdbackenddeploy.model.entity.User;
import pl.chopy.gwizdbackenddeploy.model.mapper.LocationMapper;
import pl.chopy.gwizdbackenddeploy.model.mapper.ReportMapper;
import pl.chopy.gwizdbackenddeploy.model.repository.AnimalRepository;
import pl.chopy.gwizdbackenddeploy.model.repository.LocationRepository;
import pl.chopy.gwizdbackenddeploy.model.repository.ReportRepository;
import pl.chopy.gwizdbackenddeploy.model.repository.UserRepository;
import pl.chopy.gwizdbackenddeploy.rest.auth.OidcAuthService;
import pl.chopy.gwizdbackenddeploy.rest.location.LocationService;

import java.util.List;

@Service
@AllArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final ReportMapper reportMapper;
    private final LocationMapper locationMapper;
    private final LocationService locationService;
    private final OidcAuthService oidcAuthService;

    public void addReport(ReportAddRequest request) {
        var user = oidcAuthService.getCurrentUser();
        var animal = Option.ofOptional(animalRepository.findById(request.getAnimalId()))
                .getOrElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Animal '" + request.getAnimalId() + "' not found."));
        var location = Option.of(request.getLocation())
                .map(locationMapper::map)
                .map(locationRepository::save)
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        Option.of(request)
                .map(reportMapper::map)
                .peek(rep -> rep.setAnimal(animal))
                .peek(rep -> rep.setAuthor(user))
                .peek(rep -> rep.setLocation(location))
                .map(reportRepository::save)
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
    }

    public List<SingleReportResponse> getReports(
            Long animalId, ReportType reportType, Double distanceRange, LocationAddRequest location, Long userId) {
        Animal animal = null;
        if (animalId != null) {
            animal = Option.ofOptional(animalRepository.findById(animalId))
                    .getOrElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Animal '" + animalId + "' not found."));
        }
        User user = null;
        if (userId != null) {
            user = Option.ofOptional(userRepository.findById(userId))
                    .getOrElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User '" + animalId + "' not found."));
        }
        var reportsJpa = reportRepository.getReportsByAnimalOrReportTypeOrAuthor(
                animal,
                reportType,
                user
        );
        if (distanceRange != null && location != null) {
            var loc = locationMapper.map(location);
            locationRepository.save(loc);
            reportsJpa = reportsJpa
                    .parallelStream()
                    .filter(rep -> locationService.isLocationsInRange(loc, rep.getLocation(), distanceRange))
                    .toList();
        }
        return reportsJpa
                .parallelStream()
                .map(reportMapper::map)
                .toList();
    }
}
