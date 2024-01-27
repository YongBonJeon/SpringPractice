package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Team;

import java.util.List;

@Repository
public class TeamJpaRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(Team team) {
        em.persist(team);
    }

    public Team find(Long id) {
        return em.find(Team.class, id);
    }

    public List<Team> findAll() {
        return em.createQuery("select t from Team t", Team.class)
                .getResultList();
    }

    public void delete(Team team) {
        em.remove(team);
    }

    public long count() {
        return em.createQuery("select count(t) from Team t", Long.class)
                .getSingleResult();
    }


}
