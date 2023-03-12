package hello.hellospring.domain;

import javax.persistence.*;

// JPA가 관리하는 Entity
@Entity
public class Member {

    // DB가 알아서 생성 (key?)
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
