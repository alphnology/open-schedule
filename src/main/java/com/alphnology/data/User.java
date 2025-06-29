package com.alphnology.data;

import com.alphnology.data.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

@Getter
@Setter
@Entity
@ToString
@Table(
        name = "users",
        indexes = {
                @Index(columnList = "username, name")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"username"})
        }
)
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long code;

    @Email
    @NotNull
    @Size(min = 1, max = 100)
    @Column(unique = true, updatable = false)
    private String username;

    @NotNull
    @JsonIgnore
    @Size(min = 1, max = 250)
    private String password;

    @NotNull
    @Size(min = 1, max = 100)
    private String name;

    @Size(max = 30)
    private String phone;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Role roles;

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "users")
    private List<SessionRating> ratings = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favoriteSessions")
    private Set<Session> favoriteSessions = new HashSet<>();

    private Instant lastLoginTs;

    private boolean oneLogPwd;

    private boolean locked;


    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        User that = (User) o;
        return Objects.equals(getCode(), that.getCode());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }


}
