package hello.core.member;

public class MemberServiceImpl implements MemberService {

    // 추상화에도 의존, 구현 객체에도 의존 - DIP 위반
    // 현재 : memoryMemberRepository에 대한 정보가 없다! -> DIP
    private final MemberRepository memberRepository;

    public MemberServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void join(Member member) {
        memberRepository.save(member);
    }

    @Override
    public Member findMember(Long memberId) {
        return memberRepository.findById(memberId);
    }
}
