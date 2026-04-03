package com.atamanahmet.cinelog.domain.POJO;

import java.util.ArrayList;
import java.util.List;

import com.atamanahmet.cinelog.domain.SearchEntry;
import com.atamanahmet.cinelog.domain.UserList;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    public User(User user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Username can not be empty")
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = true, unique = true)
    private String JWToken;

    @NotBlank(message = "Password can not be empty")
    @Size(min = 3)
    @Column(nullable = false, unique = true)
    private String password;

    @Column(nullable = true, unique = true)
    private String profilePicturePath;

    @Embedded
    @Column(nullable = true)
    private UserList userList;

    @ElementCollection
    @CollectionTable(name = "user_search_entries", joinColumns = @JoinColumn(name = "user_id"))
    private List<SearchEntry> searchDataWithDate = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "user_recommendation", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "movie_id"))
    private List<Movie> recommendation = new ArrayList<>();

}
