package study.security1.config.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import study.security1.model.User;
import study.security1.repository.UserRepository;

@Service
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Autowired
    private UserRepository userRepository;

    // 구글로 부터 받은 userRequest 데이터에 대한 후처리되는 함수
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("userRequest.getClientRegistration() = " + userRequest.getClientRegistration()); // registrationId로 어떤 OAuth로 로그인 했는지 확인 가능
        System.out.println("userRequest.getAccessToken() = " + userRequest.getAccessToken().getTokenValue()); // 토큰값 확인 가능

        OAuth2User oAuth2User = super.loadUser(userRequest);
        // 구글 로그인 버튼 클릭 -> 구글 로그인 창 -> 로그인 완료 -> code를 리턴(OAuth-Client라이브러리) -> AccessToken 요청
        // userRequest 정보 -> loadUser 함수 호출 -> 구글로부터 회원프로필을 받아준다.
        System.out.println("oAuth2User.getAttributes() = " + oAuth2User.getAttributes());

        String provider = userRequest.getClientRegistration().getRegistrationId(); // google
        String providerId = oAuth2User.getAttribute("sub"); // google의 primary key
        String username = provider + "_" + providerId; // google_123456789
        String password = passwordEncoder().encode("겟인데어");
        String email = oAuth2User.getAttribute("email");
        String role = "ROLE_USER";

        User userEntity = userRepository.findByUsername(username);
        if(userEntity == null) {
            userEntity = User.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .role(role)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            userRepository.save(userEntity);
        }

        return new PrincipalDetails(userEntity, oAuth2User.getAttributes());
    }
}
