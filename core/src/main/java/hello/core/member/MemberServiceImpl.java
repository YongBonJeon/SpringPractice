package hello.core.member;

public class MemberServiceImpl implements MemberService {

    // 추상화에도 의존, 구현 객체에도 의존 - DIP 위반
    private final MemberRepository memberRepository = new MemoryMemberRepository();

    @Override
    public void join(Member member) {
        memberRepository.save(member);
    }

    @Override
    public Member findMember(Long memberId) {
        return memberRepository.findById(memberId);
    }
}
