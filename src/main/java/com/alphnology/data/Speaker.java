package com.alphnology.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@ToString
@Table(
        name = "speakers",
        indexes = {
                @Index(columnList = "name")
        }
)
public class Speaker implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long code;

    @NotNull
    @Size(min = 1, max = 100)
    private String name;

    @NotNull
    @Size(min = 1, max = 100)
    private String title;

    @Size(max = 100)
    private String company;

    @NotNull
    @Size(min = 1, max = 5)
    private String country;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "networking")
    private Set<String> networking = new HashSet<>();

    @Size(max = 3000)
    private String bio;

    @Size(max = 200)
    private String photoUrl;

    @ManyToMany(mappedBy = "speakers", fetch = FetchType.EAGER)
    @ToString.Exclude
    private Set<Session> sessions = new HashSet<>();

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Speaker that = (Speaker) o;
        return Objects.equals(getCode(), that.getCode());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }


}
