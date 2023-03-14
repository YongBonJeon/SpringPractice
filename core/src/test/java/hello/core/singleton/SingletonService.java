package hello.core.singleton;

import org.junit.jupiter.api.Test;

public class SingletonService {
    // static 영역에 객체 딱 1개 생성
    private static final SingletonService instance = new SingletonService();
    // 객체 인스턴스가 필요하면 이 static 메서드를 통해서만 조회
    public static SingletonService getInstance(){
        return instance;
    }
    // 외부에서 new 키워드에서 객체 생성 불가
    private SingletonService(){
    }
}
