package com.alphnology.data;

import com.alphnology.data.enums.Language;
import com.alphnology.data.enums.Level;
import com.alphnology.data.enums.SessionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Entity
@ToString
@Table(name = "sessions", indexes = {@Index(columnList = "title"), @Index(columnList = "startTime, endTime"), @Index(columnList = "type")})
public class Session implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long code;

    @NotNull
    @Size(min = 1, max = 100)
    private String title;

    @NotNull
    @Size(min = 1, max = 3000)
    private String description;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private Level level;

    @Enumerated(EnumType.STRING)
    private Language language;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SessionType type = SessionType.T;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room", referencedColumnName = "code")
    private Room room;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "track", referencedColumnName = "code")
    private Track track;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "session_speaker",
            joinColumns = @JoinColumn(name = "session_code"),
            inverseJoinColumns = @JoinColumn(name = "speaker_id")
    )
    private Set<Speaker> speakers = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "session_tag",
            joinColumns = @JoinColumn(name = "session_code"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "session")
    private List<SessionRating> ratings = new ArrayList<>();

    public boolean blocksAllRooms() {
        return type != SessionType.T && type != SessionType.W;
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("The start time must be before the end time.");
        }

//        if (type != SessionType.CB && type != SessionType.L && type != SessionType.OS && (room == null || track == null)) {
//            throw new IllegalArgumentException("Regular sessions must have a room and a track.");
//        }
//
//        if (type == SessionType.CB || type == SessionType.L || type == SessionType.OS && (level != null || language != null)) {
//            throw new IllegalArgumentException("Coffee breaks, lunch and open space should not have level or language.");
//        }
    }

    public double getAverageRating() {
        return ratings.stream().mapToInt(SessionRating::getScore).average().orElse(0.0);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Session that = (Session) o;
        return Objects.equals(getCode(), that.getCode());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }


}
