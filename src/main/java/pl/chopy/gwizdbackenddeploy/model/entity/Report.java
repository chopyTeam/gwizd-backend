package pl.chopy.gwizdbackenddeploy.model.entity;

import lombok.Data;
import pl.chopy.gwizdbackenddeploy.model.ReportType;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Data
@Entity
public class Report {
    @Id
    @GeneratedValue
    private Long id;
    @Enumerated(EnumType.STRING)
    private ReportType reportType;
    @ManyToOne
    private Location location;
    @ManyToOne
    private Animal animal;
    private Integer quantity;
    @NotEmpty
    private String title;
    @ManyToOne
    private User author;
    private String description;
    private String photo;
    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime sleepDate = this.createdDate.plusHours(1);
    private boolean isActive = true;
}
