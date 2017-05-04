package models;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class Role extends Model{

    @Id
    private Long id;

    @Constraints.Required(message = "ce champ est obligatoire.")
    @Column(length = 100, nullable = false, unique = true)
    private String description;

    public Role() {
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static Finder<Long,Role> find = new Finder<Long,Role>(Long.class, Role.class);
}
