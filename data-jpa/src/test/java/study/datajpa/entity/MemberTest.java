package study.datajpa.entity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberTest {

    @PersistenceContext
    EntityManager em;

    @Test
    public void test() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        // 초기화
        em.flush(); // 영속성 컨텍스트에 있는 쿼리를 DB에 반영
        em.clear(); // 영속성 컨텍스트 초기화

        // 확인
        Member findMember1 = em.find(Member.class, member1.getId());
        Member findMember2 = em.find(Member.class, member2.getId());
        Member findMember3 = em.find(Member.class, member3.getId());
        Member findMember4 = em.find(Member.class, member4.getId());

        // 검증
        assertEquals(findMember1.getTeam().getName(), "teamA");
        assertEquals(findMember2.getTeam().getName(), "teamA");
        assertEquals(findMember3.getTeam().getName(), "teamB");
        assertEquals(findMember4.getTeam().getName(), "teamB");

        assertEquals(findMember1.getTeam().getId(), teamA.getId());
        assertEquals(findMember2.getTeam().getId(), teamA.getId());
        assertEquals(findMember3.getTeam().getId(), teamB.getId());
        assertEquals(findMember4.getTeam().getId(), teamB.getId());
    }
}