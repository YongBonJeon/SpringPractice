package hello.hellospring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // home 맵핑 , 맵핑된게 있으므로 웰컴 페이지(index.html)로 가지 않는다!
    @GetMapping("/")
    public String home(){
        // -> home.html
        return "home";
    }

}
