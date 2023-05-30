package jpabook.jpashop;

import jpabook.jpashop.domain.Member;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Test
    @Transactional // JPA는 무조건 트랜잭션 안에서 실행되어야 한다
    public void testMember() {
        //given
        Member member = new Member();
        member.setName("memberA");

        //when
        Long saveId = memberRepository.save(member);


        //then
        Member findMember = memberRepository.find(saveId);

        Assertions.assertThat(member.getId())
                .isEqualTo(findMember.getId());
        // JPA 엔티티 동일성 보장
        // Equals, Hash를 구현하지 않아도 같은 트랜잭션 내에서
        // 영속성 컨테스트에서 관리하기 때문에 True가 나와야 한다.
        Assertions.assertThat(findMember)
                .isEqualTo(member);

    }
}