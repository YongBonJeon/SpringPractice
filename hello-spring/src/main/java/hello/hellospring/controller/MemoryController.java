package hello.hellospring.controller;

import hello.hellospring.domain.Member;
import hello.hellospring.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;


@Controller // 스프링이 관리한다!, 스프링 컨테이너 등록
public class MemoryController {
    private final MemberService memberService;

    @Autowired // 스프링 컨테이너에서 memberService를 가져온다
    public MemoryController(MemberService memberService) {
        this.memberService = memberService;
    }

    // member/new 주소에 들어가면 리턴값.html로 이동
    @GetMapping("/members/new")
    public String createForm(){
        return "members/createMemberForm";
    }

    // members/createMemberForm에서 post 방식으로 값이 전달됨
    @PostMapping("/members/new")
    // form으로 html에서 등록한 정보가 넘어올 것
    public String create(MemberForm form){
        Member member = new Member();
        member.setName(form.getName());

        // 멤버 객체 만들어서 멤버 서비스에 회원 가입
        memberService.join(member);

        // 다시 홈으로
        return "redirect:/";
    }

    @GetMapping("/members")
    public String list(Model model){
        List<Member> members = memberService.findMembers();
        model.addAttribute("members",members);

        return "members/memberList";
    }
}
