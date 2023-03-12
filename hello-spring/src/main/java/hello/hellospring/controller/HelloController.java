package hello.hellospring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloController {
    // hello 랑 맵핑
    @GetMapping("hello")
    public String hello(Model model){
        model.addAttribute("data","spring!!");
        // templates/hello.html 연동
        return "hello";
    }

    @GetMapping("hello-mvc")
    public String helloMvc(@RequestParam("name") String name, Model model){
        model.addAttribute("name",name);
        return "hello-template";
    }

    @GetMapping("hello-string")
    @ResponseBody
    public String helloString(@RequestParam("name") String name){
        return "hello " + name; // 그대로 올라감
    }


    // JSON 방식 (Key, value)
    @GetMapping("hello-api")
    @ResponseBody
    // template 찾지 않고 HTTP:BODY에 내용 직접 변환 및 전달
    public Hello helloApi(@RequestParam("name") String name){
        Hello hello = new Hello();
        hello.setName(name);
        return hello;
    }

    static class Hello{
        private String name;

        public void setName(String name) {
            this.name = name;
        }

        public String getName(){
            return name;
        }
    }
}
