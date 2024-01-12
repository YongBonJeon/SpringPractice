## 객체 지향 프로그래밍

**역할**과 **구현**으로 분리
- 클라이언트는 **대상의 역할(인터페이스)만** 알면 된다.
- 클라이언트는 구현 대상의 내부 구조를 몰라도 된다.
- 클라이언트는 구현 대상의 내부 구조가 바뀌어도 상관 없다.
- 클라이언트는 구현 대상 자체가 변경되어도 상관 없다.

**In Java,**
- 역할 : 인터페이스
- 구현 : 인터페이스를 구현한 클래스

객체를 설계할 때 역할, 구현을 명확히 분리하고 설계 시 역할을 부여하여 역할을 수행하는 구현 객체 만든다!

### 자바 언어의 다형성
- **오버라이딩**을 통해 인터페이스를 구현할 수 있고 다형성을 통해 실행 시점에 인터페이스를 구현한 객체를 유연하게 변경할 수 있다.
- 클라이언트를 변경하지 않고 서버의 구현 기능을 유연하게 변경할 수 있는 것이 다형성의 **본질**
  ![](https://velog.velcdn.com/images/bon0057/post/d33bc9d5-62dd-438c-9529-c7d5a45d8dc8/image.jpeg)


### 좋은 객체 지향 프로그래밍 - SOLID
**1. SRP 단일 책임 원칙 (Single responsibility principle)**
- 한 클래스는 하나의 책임만 가져야 한다.

**2. OCP 개방-폐쇄 원칙 (Open/closed principle) **
- 확장에는 열려 있으나 변경에는 닫혀 있어야 한다.
- 구현 객체를 변경하려면 클라이언트 코드를 **변경**해야 한다.
  분명 다형성을 사용했지만 **OCP를 위반**할 수 밖에 없다.
```java
/*
현재 클라이언트가 구현 클래스를 직접 선택하고 있기 때문에 
새로운 구현 객체를 만들고 변경하는 과정에서 클라이언트 코드를 변경해야 한다.
*/
//private MemberRepository memberRepository = new MemoryMemberRepository();
private MemberRepository memberRepository = new JdbcMemberRepository();
```
-> 별도의 조립, 설정자가 필요하다! ~~Spring이 해준다~~

**3. LSP 리스코프 치환 원칙 (Liskov substitution principle)**
- 프로그램의 객체는 프로그램의 정확성을 깨뜨리지 않으면서 하위 타입의 인스턴스로 바꿀 수 있어야 한다.
- 다형성에서 하위 클래스는 인터페이스 규약을 다 지켜야 한다는 것이다. 인터페이스를 구현한 구현체는 믿고 사용하기 위해 이 원칙이 필요하다.

**4. ISP 인터페이스 분리 원칙 (Interface segregation principle)**
- 특정 클라이언트를 위한 인터페이스 여러 개가 범용 인터페이스 하나보다 낫다
  ex) 자동차 인터페이스 -> 운전 인터페이스, 정비 인터페이스 분리

**5. DIP 의존관계 역전 원칙 (Dependency inversion principle)**
- 추상화에 의존, 구체화에 의존 X
- 클라이언트가 인터페이스에 의존해야 유연하게 구현체를 변경할 수 있다.
- MemberRepository m = new **Memory**MemberRepository();
  -> **DIP 위반!**

>
객체 지향의 핵심은 **다형성**이지만 다형성만으로는 OCP, DIP를 지킬 수 없다. 무엇인가 더 필요하다! -> **스프링과 같은 프레임워크의 등장**

## 회원 도메인 예제 설계 - 순수 자바

![](https://velog.velcdn.com/images/bon0057/post/8057f25c-bcc0-4b88-bb40-a80b84c625c7/image.png)

### 요구사항
- 회원가입, 조회 가능
- 회원 등급 : 일반, VIP
- 회원 데이터는 자체 DB(메모리), 외부 시스템(DB)과 연동 가능


**1. 회원 객체**
```java
public class Member {
    private Long id;
    private String name;
    private Grade grade;

    public Member(Long id, String name, Grade grade) {
        this.id = id;
        this.name = name;
        this.grade = grade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }
}
```

**2. 회원 서비스 인터페이스**
```java
public interface MemberService {
    void join(Member member);

    Member findMember(Long memberId);
}
```
**3. 회원 서비스 구현 객체**
```java
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
```

**4. 회원 레퍼지토리 인터페이스**
```java
public interface MemberRepository {
    void save(Member member);

    Member findById(Long memberId);
}
```
**5. 회원 레퍼지토리(메모리) 구현 객체**
```java
public class MemoryMemberRepository implements MemberRepository {

    private static Map<Long, Member> store = new HashMap<>();

    @Override
    public void save(Member member) {
        store.put(member.getId(), member);
    }

    @Override
    public Member findById(Long memberId) {
        return store.get(memberId);
    }
}
```
**6. 회원 가입 및 조회 실행**
```java
public class MemberApp {
    public static void main(String[] args) {
        MemberService memberService = new MemberServiceImpl();
        Member member = new Member(1L, "memberA", Grade.VIP);
        memberService.join(member);

        Member findMember = memberService.findMember(1L);
        System.out.println("new " + member.getName());
        System.out.println("test : " + findMember.getName());
    }
}
```
>Spring framework의 어떠한 도움도 받지 않고 **순수한 자바 코드**로 만든 회원 도메인이다.

### 문제사항
- DIP 및 OCP 위반
```java
private final MemberRepository memberRepository = new *Memory*MemberRepository();
```

## 주문 및 할인 도메인 예제 설계 - 순수 자바
![](https://velog.velcdn.com/images/bon0057/post/b43fe656-d034-4d48-9a00-56f24d4de93c/image.png)


### 요구사항
- 회원은 상품 주문 가능
- 회원 등급에 따라 할인 정책 적용
- VIP는 1000원을 할인해주는 고정 금액 할인 적용(추후 변동 가능)

**1. 주문 객체**
```java
public class Order {
    private Long memberId;
    private String itemName;
    private int itemPrice;
    private int discountPrice;

    public Order(Long memberId, String itemName, int itemPrice, int discountPrice) {
        this.memberId = memberId;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.discountPrice = discountPrice;
    }

    public int calculatePrice(){
        return itemPrice-discountPrice;
    }
    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(int itemPrice) {
        this.itemPrice = itemPrice;
    }

    public int getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(int discountPrice) {
        this.discountPrice = discountPrice;
    }

    @Override
    public String toString() {
        return "Order{" +
                "memberId=" + memberId +
                ", itemName='" + itemName + '\'' +
                ", itemPrice=" + itemPrice +
                ", discountPrice=" + discountPrice +
                '}';
    }
}
```
**2. 주문 서비스 인터페이스**
```java
public interface OrderService {
    Order createOrder(Long memberId, String itemName, int itemPrice);
}

```
**3. 주문 서비스 구현 객체**
```java
public class OrderServiceImpl implements OrderService{

    private final MemberRepository memberRepository = new MemoryMemberRepository();
    private final DiscountPolicy discountPolicy = new FixDiscountPolicy();

    @Override
    public Order createOrder(Long memberId, String itemName, int itemPrice) {
        Member member = memberRepository.findById(memberId);
        int discountPrice = discountPolicy.discount(member, itemPrice);

        return new Order(memberId, itemName, itemPrice, discountPrice);
    }
}
```
**4. 할인 서비스 인터페이스**
```java
public interface DiscountPolicy {
    /*
    return 할인 대상 금액
     */
    int discount(Member member, int price);
}
```
**5-1. 고정 할인 서비스 구현 객체**
```java
public class FixDiscountPolicy implements DiscountPolicy {

    private int discountFixAmount = 1000;
    @Override
    public int discount(Member member, int price) {
        if(member.getGrade() == Grade.VIP){
            return discountFixAmount;
        }
        else {
            return 0;
        }
    }
}
```
**5-2. 가변 할인 서비스 구현 객체**
```java
public class RateDiscountPolicy implements DiscountPolicy{

    private int discountPercent = 10;
    @Override
    public int discount(Member member, int price) {
        if(member.getGrade() == Grade.VIP){
            return price*discountPercent/100;
        }
        else{
            return 0;
        }
    }
}
```

**6. 주문 및 할인 실행**
```java
public class OrderApp {
    public static void main(String[] args) {
        MemberService memberService = new MemberServiceImpl();
        OrderService orderService = new OrderServiceImpl();

        Long memberId = 1L;
        Member member = new Member(memberId, "memberA", Grade.VIP);
        memberService.join(member);

        Order order = orderService.createOrder(memberId,"itemA",10000);

        System.out.println("order + " + order);
    }
}
```

### 문제사항
- 정책에 의해 고정 금액 할인에서 가변 금액 할인으로 변경될 때 클라이언트 코드를 변경해야 한다 -> OCP 위반
- 인터페이스에 의존하는 것이 아닌 **구현 객체**에 의존하고 있다. -> DIP 위반
```java
 // private final DiscountPolicy discountPolicy = new FixDiscountPolicy();
    private final DiscountPolicy discountPolicy = **new RateDiscountPolicy();**
```
![](https://velog.velcdn.com/images/bon0057/post/21d1c9ea-91b0-4862-a33d-e97141260239/image.png)

- 인터페이스에만 의존하게 할 수는 있으나 구현체가 없어서 정상적인 실행을 할 수 없다.
```java
//	private final DiscountPolicy discountPolicy = new RateDiscountPolicy();
    private DiscountPolicy discountPolicy;
```
> 구현 객체를 대신해서 생성하고 주입할 무엇인가 필요하다!

## AppConfig 등장(기획자)
- 애플리케이션의 전체 동장 방식을 구성(구현 객체 생성, 연결)하는 별도의 클래스이다.
```java
public class AppConfig {
    public MemberService memberService(){
        return new MemberServiceImpl(new MemoryMemberRepository());
    }

    public OrderService orderService(){
        return new OrderServiceImpl(new MemoryMemberRepository(), new FixDiscountPolicy());
    }
}
```
- Appconfig는 실제 동작에 필요한 **구현 객체를 생성**하고 **생성자를 통해 주입**한다.

**회원 서비스 구현 객체 수정(생성자 주입)**
```java
	// 과거 : 추상화에도 의존, 구현 객체에도 의존 - DIP 위반
    // 현재 : memoryMemberRepository에 대한 정보가 없다! -> DIP
    private final MemberRepository memberRepository;

    public MemberServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
```
**회원 가입 및 조희 수정(AppConfig 사용)**
```java
public class MemberApp {
    public static void main(String[] args) {
        AppConfig appConfig = new AppConfig();
        MemberService memberService = appConfig.memberService();

        Member member = new Member(1L, "memberA", Grade.VIP);
        memberService.join(member);

        Member findMember = memberService.findMember(1L);
        System.out.println("new " + member.getName());
        System.out.println("test : " + findMember.getName());
    }
}
```
주문 및 할인 서비스 구현 객체 또한 똑같이 수정했다.
![](https://velog.velcdn.com/images/bon0057/post/1381ebf6-60c5-4eef-b851-64953f21a367/image.png)


- 회원 서비스는 이제 구현 객체를 의존하지 않고 인터페이스에만 의존한다!
- AppConfig라는 전체를 기획하는 클래스가 어떻게 동작할 지, 어떤 구현 객체를 넣어줄 지 선택한다.

**할인 정책 변경(고정 -> 가변)**
```java
public DiscountPolicy discountPolicy(){
//      return new FixDiscountPolicy();
        return new RateDiscountPolicy();
    }
```
- 어떤 클라이언트 코드도 수정되지 않는다.

> DIP, OCP 위반 문제가 해결되었다!

## IoC(Inversion of Control)
- 기존 프로그램은 클라이언트 구현 객체가 직접 서버 구현 객체를 생성, 연결, 실행했다. 반면에 AppConfig가 등장한 이후에 구현 객체는 각자의 역할만 담당하고 전체를 제어하는 흐름은 AppConfig가 가져간다. 이렇게 프로그램의 제어 흐름을 직접 제어하는 것이 아닌 외부에서 관리하는 것이 **제어의 역전**이라고 한다.

## DI(Dependency Injection)
- 주문 서비스 객체(OrderServiceImpl)은 할인 정책(DiscountPolicy)에 의존하지만 실제 어떤 구현 객체가 사용될지는 모른다.
- 애플리케이션 실제 실행 시점(런타임)에 외부에서 실제 구현 객체를 생성하고 클라이언트에 전달해서 클라이언트와 서버의 실제 의존관계가 연결되는 것을 **의존관계 주입**이라고 한다. DI를 사용하면 정적인 클래스 의존관계를 변경하지 않고 동적인 객체 인스턴스 의존관계를 쉽게 변경할 수 있다.

> AppConfig처럼 객체를 생성하고 의존관계를 연결해주는 것을 IoC, DI 컨테이너라고 한다

## Spring 컨테이너
- AppConfig에 설정을 구성한다는 뜻의 @Configuration 을 붙여준다.
- 각 메서드에 @Bean 을 붙여준다. 이렇게 하면 스프링 컨테이너에 스프링 빈으로 등록한다.
- 스프링 컨테이너는  @Configuration 이 붙은 AppConfig 를 설정(구성) 정보로 사용한다. 여기서 @Bean 이라 적힌 메서드를 모두 호출해서 반환된 객체를 스프링 컨테이너에 등록한다. 이렇게 스프링 컨테이너에 등록된 객체를 스프링 빈이라 한다.

```java
@Configuration
public class AppConfig {

    @Bean
    public MemberService memberService(){
        return new MemberServiceImpl(memberRepository());
    }

    @Bean
    public static MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }

    @Bean
    public OrderService orderService(){
        return new OrderServiceImpl(memberRepository(), discountPolicy());
    }

    @Bean
    public DiscountPolicy discountPolicy(){
//        return new FixDiscountPolicy();
        return new RateDiscountPolicy();
    }
}
```

```java
/*AppConfig appConfig = new AppConfig();
// 수동으로 자바 객체 생성
MemberService memberService = appConfig.memberService();*/

// 스프링 컨테이너가 자동으로 객체 생생허새 스프링 Bean으로 관리
ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
// getBean(메소드이름, 반환 클래스)
MemberService memberService = applicationContext.getBean("memberService", MemberService.class);
```

### 스프링 컨테이너 생성 과정
**1. 스프링 컨테이너 생성**
![](https://velog.velcdn.com/images/bon0057/post/4ffe044c-e4e7-4f50-a973-9a2f46e4b0e6/image.png)
```java
// AnnotationConfigApplicationContext(구성 정보);
ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
MemberService memberService = applicationContext.getBean("memberService", MemberService.class);
```

**2. 스프링 빈 등록**
![](https://velog.velcdn.com/images/bon0057/post/eab4a567-a1d0-4d6a-80b9-bf3a8cae3986/image.png)
- @Bean 어노테이션이 붙은 메소드를 모두 실행해서 나온 객체들을 스프링 빈을 등록한다.

**3.스프링 빈 의존관계 설정 - 준비**
![](https://velog.velcdn.com/images/bon0057/post/9ffb3075-4fd9-426c-9273-22089ce1f394/image.png)

**4.스프링 빈 의존관계 설정 - 완료**
![](https://velog.velcdn.com/images/bon0057/post/666c39dc-c825-454d-a864-e63b59d37073/image.png)

### 스프링 Bean 조회 방식
- 스프링컨테이너.getBean(빈 이름, 빈 타입)
- 스프링컨테이너.getBean(빈 타입) -> 같은 타입의 빈이 여러개 있으면 예외 발생
- 스프링컨테이너.getBeanOfType(빈 타입) -> 같은 타입의 빈 전부 반환, 부모 타입으로 조회시 자식 타입의 빈들 전부 반환

### BeanFactory
- 스프링 컨테이너의 최상위 인터페이스
- 스프링 빈을 관리하고 조회하는 역할을 담당하고 getBean()을 제공한다.

### ApplicationContext
- BeanFactory 기능을 모두 상속받아서 제공
- 애플리케이션을 개발할 때 빈 관리,조회 말고도 수많은 부가기능을 제공한다.
    1. 메세지소스를 활용한 국제화 기능
    2. 환경변수
    3. 애플리케이션 이벤트
    4. 편리한 리소스 조회

## 싱글톤 패턴
- 클래스의 인스턴스가 딱 1개만 생성되는 것을 보장하는 디자인 패턴이다.
```java
	@Test
    @DisplayName("스프링 없는 순수한 DI 컨테이너")

    void pureContainer(){
        AppConfig appConfig = new AppConfig();
        //1. 조회 : 호출할 때마다 객체 생성
        MemberService memberService1 = appConfig.memberService();
        //2. 조회 : 호출할 때마다 객체 생성
        MemberService memberService2 = appConfig.memberService();

        //참조값이 다른걸 확인
        System.out.println("m1 "+memberService1);
        System.out.println("m2 "+memberService2);

        Assertions.assertThat(memberService1).isNotSameAs(memberService2);
    }
```
- 스프링 없는 순수한 DI 컨테이너인 AppConfig는 요청할 때마다 객체를 새로 생성 -> 메모리 낭비 심함

```java
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
```
```java
@Test
    @DisplayName("싱글톤 패턴 적용 ")
    void singletonServiceTest(){
        SingletonService singletonService1 = SingletonService.getInstance();
        SingletonService singletonService2 = SingletonService.getInstance();

        System.out.println("s1 "+ singletonService1);
        System.out.println("s2 "+ singletonService2);

        Assertions.assertThat(singletonService1).isSameAs(singletonService2);
    }
```
- 요청할 때마다 객체를 계속 생성하는 문제는 사라졌지만 추가로 다양한 문제가 등장한다.

### 문제사항
- 싱글톤 패턴을 구현하는 코드 자체가 많이 들어간다.
- 의존관계상 클라이언트가 구체 클래스에 의존한다. -> DIP를 위반한다.
- 클라이언트가 구체 클래스에 의존해서 OCP 원칙을 위반할 가능성이 높다.
- 테스트하기 어렵다.
- 내부 속성을 변경하거나 초기화 하기 어렵다. private 생성자로 자식 클래스를 만들기 어렵다.


### 싱글톤 컨테이너
```java
@Test
@DisplayName("스프링 컨테이너")
void springContainer(){
	// AppConfig를 기반으로 스프링 컨테이너 생성
	ApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);
   	//조회
  	MemberService memberService1 = ac.getBean("memberService", MemberService.class);
  	//조회 
	MemberService memberService2 = ac.getBean("memberService", MemberService.class);

	System.out.println("m1 "+memberService1);
	System.out.println("m2 "+memberService2);
    
    Assertions.assertThat(memberService1).isSameAs(memberService2);
}
```
- 스프링 컨테이너는 싱글톤 패턴을 적용하지 않아도, 객체 인스턴스를 싱글톤으로 관리한다.
- 싱글톤 패턴 설계를 위해 지저분한 코드를 적용하지 않아도 되어서 좋다!

### 싱글톤 방식의 주의점
```java
public class StatefulService {
    private int price;

    public void order(String name, int price){
        System.out.println(name + " " + price);
        this.price = price;
    }

    public int getPrice(){
        return price;
    }
}
```
- price가 클래스의 변수로써 stateful(계속 유지)하게 존재하고 있다

```java
@Test
void statefulServiceSingleton(){
 	ApplicationContext ac = new AnnotationConfigApplicationContext(TestConfig.class);
	StatefulService statefulService1 = ac.getBean(StatefulService.class);
	StatefulService statefulService2 = ac.getBean(StatefulService.class);

	// A
	statefulService1.order("userA",10000);
	// B
	statefulService2.order("nameB",20000);

	// A??
	int price = statefulService1.getPrice();
	System.out.println("price = "+price);

    assertThat(statefulService1.getPrice()).isEqualTo(20000);
}

static class TestConfig{
	@Bean
	public StatefulService statefulService(){
		return new StatefulService();
    }
}
```
- 특정 클라이언트가 값을 변경하여 원하지 않는 주문값이 들어가있다.
- **멀티쓰레드** 환경 등에서 치명적인 오류로 나타날 수 있다.
- 객체(스프링 빈)은 싱글톤으로 유지되기에 항상 **Stateless**하게(공유 필드 X) 설계되어야 한다!


## @Configuration
```java
@Configuration
public class AppConfig {
    @Bean
    public MemberService memberService(){
        return new MemberServiceImpl(memberRepository());
    }

    @Bean
    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }

    @Bean
    public OrderService orderService(){
        return new OrderServiceImpl(memberRepository(), discountPolicy());
    }

    @Bean
    public DiscountPolicy discountPolicy(){
//        return new FixDiscountPolicy();
        return new RateDiscountPolicy();
    }
}
```
- 스프링이 @Bean 어노테이션이 붙은 각 메소드를 실행하면서 스프링 컨테이너에 스프링 빈(객체)를 등록, 관리하는 것이라고 배웠다. 하지만 AppConfig에서 모든 메소드를 실행하면 memberRepository() 메소드는 여러번 호출된다. 객체가 여러 개 생성되고 싱글톤이 깨지는 것은 아닐까?


```java
@Test
void configurationTest(){
	ApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);

	MemberServiceImpl memberService = ac.getBean("memberService", MemberServiceImpl.class);
	OrderServiceImpl orderService = ac.getBean("orderService", OrderServiceImpl.class);
	MemberRepository memberRepository = ac.getBean("memberRepository", MemberRepository.class);

	MemberRepository memberRepository1 = memberService.getMemberRepository();
	MemberRepository memberRepository2 = orderService.getMemberRepository();

	System.out.println("memberService -> memberRepositoy "+memberRepository1);
	System.out.println("orderService -> memberRepositoy "+memberRepository2);
	System.out.println("memberRepositoy "+memberRepository);

   	Assertions.assertThat(memberService.getMemberRepository()).isSameAs(memberRepository);
   	Assertions.assertThat(orderService.getMemberRepository()).isSameAs(memberRepository);
}
```
- 테스트 결과 각 객체의 MemberRepository는 모두 같았다.
- 이는 AppConfig 클래스도 스프링 빈으로써 관리가 되고 스프링이 바이트 코드를 조작해서 싱글톤이 보장되도록 해주는 것이다. 만약 @Configuration 어노테이션이 없다면 AppConfig 클래스도 스프링 빈으로 등록이 되기는 하나 **싱글톤이 보장받을 수 없으므로** 어노테이션을 꼭 붙이도록 하자.



## 컴포넌트 스캔

Config 파일에서 기존에는 @Bean 어노테이션을 활용해서 수동으로 스프링 빈을 생성하고 의존관계를 주입했다. 하지만 스프링은 @Component, @ComponentScan을 통해 스프링 빈을 자동 생성하고 @Autowired를 통해 의존관계도 자동으로 주입해주는 아주 똑똑한 놈이다.

**기존 Config 파일**
```java
@Configuration
public class AppConfig {
    @Bean
    public MemberService memberService(){
        System.out.println("AppConfig.memberService");
        return new MemberServiceImpl(memberRepository());
    }

    @Bean
    public MemberRepository memberRepository() {
        System.out.println("AppConfig.memberRepository");
        return new MemoryMemberRepository();
    }

    @Bean
    public OrderService orderService(){
        System.out.println("AppConfig.orderService");
        return new OrderServiceImpl(memberRepository(), discountPolicy());
    }

    @Bean
    public DiscountPolicy discountPolicy(){
//        return new FixDiscountPolicy();
        return new RateDiscountPolicy();
    }
}
```

**컴포넌트 스캔 활용**
```java
@Configuration
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class)
)
public class AutoAppConfig {
}

@Component
public class RateDiscountPolicy implements DiscountPolicy

@Component
public class MemoryMemberRepository implements MemberRepository

@Component
public class MemberServiceImpl implements MemberService

@Component
public class OrderServiceImpl implements OrderService
```
- @Configruation과 @ComponentScan을 Config 파일에 붙인 후 스프링 빈으로 등록을 원하는 객체들에게는 @Component를 붙인다.
- 스프링 빈의 이름은 기본적으로 클래스명에서 맨 앞글자만 소문자로 사용한다.
- 컴포넌트 스캔은 @Component 말고도 @Controller, @Service, @Repository, @Configruation 이 붙은 대상들도 스프링 빈으로 등록한다.

**Autowired 의존관계 자동 주입**
```java
	@Autowired
    public MemberServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
	@Autowired
    public OrderServiceImpl(MemberRepository memberRepository, DiscountPolicy discountPolicy) {
        this.memberRepository = memberRepository;
        this.discountPolicy = discountPolicy;
    }
```
- 스프링이 알아서 다 해준다..

### 스프링 빈 중복 등록과 충돌
- 자동 빈 등록할 때 스프링 빈의 이름이 같은 경우 스프링이 기본적으로 오류를 발생시킨다.
- 자동으로 등록된 빈과 수동으로 등록된 빈이 이름이 같은 경우 **스프링 부트**가 오류를 발생시킨다.


## 의존 관계 주입 방식
### 생성자 주입 (추천 방식)
```java
@Component
public class OrderServiceImpl implements OrderService {
	private final MemberRepository memberRepository;
	private final DiscountPolicy discountPolicy;
  		
	@Autowired
	public OrderServiceImpl(MemberRepository memberRepository, DiscountPolicy
  discountPolicy) {
		this.memberRepository = memberRepository;
		this.discountPolicy = discountPolicy;
	}
}
```
- 생성자 호출시점에 1번 호출
- 불변, 필수 의존관계에 사용
- 생성자가 1개만 있으면 @Autowired 생략 가능
- Parameter로 필요한 객체를 @Autowired 어노테이션을 통해 스프링 빈에 등록된 객체를 자동으로 찾아서 주입해주는 것
- final 사용 가능
- Test 시에 직관적으로 사용 가능

### 수정자 주입 (setter 주입)
```java
@Component
public class OrderServiceImpl implements OrderService {
	private MemberRepository memberRepository;
	private DiscountPolicy discountPolicy;
    
   	@Autowired
	public void setMemberRepository(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}
	@Autowired
	public void setDiscountPolicy(DiscountPolicy discountPolicy) {
		this.discountPolicy = discountPolicy;
	}
}
```
- 수정자 메서드를 통해서 의존관계를 주입한다.
- **선택, 변경** 가능성이 있는 의존관계에서 사용한다.

### 필드 주입
```java
@Autowired
private MemberRepository memberRepository;
@Autowired
private DiscountPolicy discountPolicy;
```
- 추천되지 않는 방식

### 일반 메서드 주입
```java
private MemberRepository memberRepository;
private DiscountPolicy discountPolicy;

@Autowired
public void init(MemberRepository memberRepository, DiscountPolicy discountPolicy) {
	this.memberRepository = memberRepository;
	this.discountPolicy = discountPolicy;
}
 ```

### Lombok + 생성자 자동 주입
```java
@Component
public class OrderServiceImpl implements OrderService {
	private final MemberRepository memberRepository;
	private final DiscountPolicy discountPolicy;
	
    @Autowired
	public OrderServiceImpl(MemberRepository memberRepository, DiscountPolicy discountPolicy) {
		this.memberRepository = memberRepository;
		this.discountPolicy = discountPolicy;
	}
}
```
- 생성자가 1개인 경우 @Autowired를 생략해도 자동 주입이 가능하다.
- Lombok 라이브러리의 @RequriedArgsConstructor 어노테이션을 사용하면 final이 붙은 필드들의 생성자를 자동으로 만들어준다.
- 이 2가지 기능을 통해 많은 코드를 생략한 생성자 자동 주입 코드를 작성할 수 있다.

```java
@Component
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
	private final MemberRepository memberRepository;
	private final DiscountPolicy discountPolicy;
}
```

## 자동 주입 시 조회 빈이 2개 이상인 경우
- @Autowired는 **타입으로 조회**한다. getBean(class.class)처럼 작동하기 대문에 여러 빈이 동시에 조회될 수 있다.

### 필드명 매칭
```java
@Autowired
private DiscountPolicy discountPolicy

@Autowired
private DiscountPolicy rateDiscountPolicy
```
- 필드명 매칭은 여러 빈이 조회가 된 경우 필드명으로 매칭하려고 시도한다.

### @Qualifier
```java
@Component
@Qualifier("mainDiscountPolicy")
public class RateDiscountPolicy implements DiscountPolicy {}

@Autowired
public OrderServiceImpl(MemberRepository memberRepository,
                          @Qualifier("mainDiscountPolicy") DiscountPolicy discountPolicy) {
	this.memberRepository = memberRepository;
	this.discountPolicy = discountPolicy;
}
```
- @Qualifier끼리 매칭한다.
- 매칭 실패 시 빈 이름으로 매칭한다.

### @Primary
```java
@Component
@Primary
public class RateDiscountPolicy implements DiscountPolicy {}
 
@Component
public class FixDiscountPolicy implements DiscountPolicy {}
```
- @Primary 어노테이션이 붙은 빈이 우선적으로 주입된다.

### 2개 이상의 모든 빈 조회
```java
static class DiscountService{
        private final Map<String, DiscountPolicy> policyMap;
        private final List<DiscountPolicy> policies;

        @Autowired
        public DiscountService(Map<String, DiscountPolicy> policyMap, List<DiscountPolicy> policies) {
            this.policyMap = policyMap;
            this.policies = policies;
            System.out.println("policyMap = " + policyMap );
        }

        public int discount(Member member, int price, String discountCode) {
            DiscountPolicy discountPolicy = policyMap.get(discountCode);
            return discountPolicy.discount(member, price);
        }
    }
```
- Map과 List를 사용하여 모든 빈을 조회하고 자동 주입 받을 수 있다.


## 빈 생명주기 콜백

- 스프링 빈은 **객체생성 -> 의존관계 주입**이라는 라이프 사이클을 가진다.
- 이 사이클이 끝나고 난 후에 초기화 등 다양한 작업을 호출해야 하기에 스프링은 초기화, 소멸의 시점을 알려주는 콜백을 주는 기능을 갖고 있다.

**스프링 빈의 이벤트 라이프 사이클**
- 객체생성 -> 의존관계 주입 -> 초기화 콜백 -> 사용 -> 소멸 전 콜백 -> 스프링 종료

> **객체의 생성과 초기화 분리**
생성자는 필수 정보(파라미터)를 통해 메모리를 할당하고 객체를 생성하는 책임을 갖고 있다. 생성자 안에 무거운 초기화 작업을 함께 하는 것보다 객체 생성, 초기화를 명확하게 구분하는 것이 좋다!

### 콜백 - 인터페이스(InitializingBean, DisposableBean)
```java
public class Network implements InitializingBean, DisposableBean{
	
    @Override
    public void afterPropertiesSet() throws Exception{
    	// 초기화 기능
    }
    
    @Override
    public void destory() throws Exception{
    	// 소멸 전 기능
    }
}
```
- InitializingBean 은 afterPropertiesSet() 메소드로 초기화를 지원한다.
- DisposableBean 은 Destory() 메소드로 초기화를 지원한다.
- 스프링 전용 인터페이스이며** 메소드의 이름을 바꿀 수 없다는 단점**이 있다.
- 굉장히 옛날에 만들어진 방식이므로 추천되지 않음

### 콜백 - 빈 등록시 초기화, 소멸 메소드 지정
```java
public class NetworkClient {
	public void init() {
    	// 초기화 기능
    }
    
    public void close() {
    	// 소멸 전 기능
    }
}

@Configuration
static class LifeCycleConfig {
	@Bean(initMethod = "init", destroyMethod = "close")
	public NetworkClient networkClient() {
		NetworkClient networkClient = new NetworkClient();
		networkClient.setUrl("http://hello-spring.dev");
		return networkClient;
	}
}
```
- @Bean 어노테이션에 초기화 메소드, 소멸 메소드를 지정하는 방식으로 초기화, 소멸을 지원한다.
- 메소드의 이름을 바꿀 수 있다.
- destroyMethod는 추론이 기본값이므로 따로 지정하지 않아도 close, shutdown이라는 이름의 메소드를 기본적으로 작동한다.

### 콜백 - @PostConstruct, @PreDestroy
```java
public class NetworkClient {
	
    @PostConstruct
	public void init() {
    	// 초기화 기능
    }
    
    @PreDestory
    public void close() {
    	// 소멸 전 기능
    }
}

@Configuration
static class LifeCycleConfig {
	@Bean
	public NetworkClient networkClient() {
		NetworkClient networkClient = new NetworkClient();
		networkClient.setUrl("http://hello-spring.dev");
		return networkClient;
	}
}
```
- 최신 스프링에서 가장 권장하는 방식이며 매우 편리하게 작동한다.
- 스프링에 종속적인 기술이 아니므로 다른 컨테이너에서도 동작한다.
- 컴포넌트 스캔과 조합이 좋다.

## 빈 스코프
- 빈의 스코프는 빈이 존재할 수 있는 범위를 의미한다.

### 싱글톤 스코프
- 기본값 스코프이며 스프링 컨테이너의 생성부터 소멸 시점까지 함께하는 스프링 빈이다.

### 프로토타입 스코프
- 기본값으로 사용되는 싱글톤 스코프의 빈은 조회할 때마다 컨테이너에서 항상 같은 인스턴스의 스프링 빈을 반환한 반면에 프로토타입 스코프의 빈은 **조회할 때마다 항상 새로운 인스턴스를 생성, 반환**한다.

1. 프로토타입 스코프의 빈을 컨테이너에 요청
2. 요청받은 시점에 컨테이너는 프로토타입 빈 생성, 필요한 의존관계 주입
3. 빈을 클라이언트에 반환

- 스프링 컨테이너는 프로토타입 빈을 생성, 의존관계 주입, 초기화까지는 처리하지만 클라이언트에 빈을 반환한 후에 프로토타입 빈을 관리하지 않는다. **반환 후의 책임은 클라이언트에 존재**하며 소멸 전 콜백 등의 기능을 제공하지 않는다.

```java
	@Test
    void prototypeBeanFind(){
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(PrototypeBean.class);

        PrototypeBean bean1 = ac.getBean(PrototypeBean.class);
        PrototypeBean bean2 = ac.getBean(PrototypeBean.class);

        System.out.println("sing1 = " + bean1);
        System.out.println("sing2 = " + bean2);
        Assertions.assertThat(bean1).isNotSameAs(bean2);

        ac.close();
    }

    @Scope("prototype")
    static class PrototypeBean{
        @PostConstruct
        public void init(){
            System.out.println("PrototypeBean.init");
        }

        @PreDestroy
        public void destroy(){
            System.out.println("PrototypeBean.destroy");
        }
    }
```
![](https://velog.velcdn.com/images/bon0057/post/1d7fed07-0683-43ce-9443-ea79f00fe73e/image.png)

- 싱글톤 빈은 스프링 컨테이너 생성 시점에 초기화 메서드가 실행 되지만, 프로토타입 스코프의 빈은 스프링 컨테이너에서 **빈을 조회할 때 생성되고, 초기화 메서드도 실행**된다.
- 프로토타입 빈을 2번 조회했으므로 완전히 다른 스프링 빈이 생성되고, 초기화도 2번 실행된 것을 확인할 수 있다.
- 싱글톤 빈은 스프링 컨테이너가 관리하기 때문에 스프링 컨테이너가 종료될 때 빈의 종료 메서드가 실행되지만, 프로토타입 빈은 스프링 컨테이너가 생성과 의존관계 주입 그리고 초기화 까지만 관여하고, 더는 관리하지 않는다. 따라서 프로토타입 빈은 스프링 컨테이너가 종료될 때 @PreDestroy 같은 종료 메서드가 전혀 실행되지 않는다.

### 싱글톤 빈에 프로토타입 빈을 함께 사용하는 경우
```java
@Test
void singletonClientUsePrototype(){
	AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(PrototypeBean.class,ClientBean.class);
	ClientBean clientBean1 = ac.getBean(ClientBean.class);
	int count1 = clientBean1.logic();
	Assertions.assertThat(count1).isEqualTo(1);

	ClientBean clientBean2 = ac.getBean(ClientBean.class);
	int count2 = clientBean2.logic();
	Assertions.assertThat(count2).isEqualTo(2);
}

@Scope("singleton")
@Component
@RequiredArgsConstructor
static class ClientBean{

	private final PrototypeBean provider;

	public int logic(){
		prototypeBean.addCount();
		return prototypeBean.getCount();
	}
}


@Scope("prototype")
    @Component
    static class PrototypeBean{
        private int count = 0;

        public void addCount(){
            count++;
        }

        public int getCount(){
            return count;
        }

        @PostConstruct
        public void init(){
            System.out.println("PrototypeBean.init " + this);
        }

        @PreDestroy
        public void destroy(){
            System.out.println("PrototypeBean.destroy");
        }
    }
```
- ClientBean은 싱글톤 빈이므로 컨테이너 생성 시점에 생성되고 자동 주입이 이루어지고 자동 주입 시에 프로토타입 빈이 생성되고 주입된다.
- 이후에 ClientBean을 조회할 때 내부에 갖고 있는 프로토타입 빈은 이미 주입이 끝난 빈이므로 계속해서 사용된다. 조회할 때마다 새로운 프로토타입 빈이 생성되는 것이 아니다.

#### 문제 해결 : 스프링 컨테이너에 새로 요청
```java
static class ClientBean {
	@Autowired
	private ApplicationContext ac;
          
	public int logic() {
		PrototypeBean prototypeBean = ac.getBean(PrototypeBean.class);
		prototypeBean.addCount();
		int count = prototypeBean.getCount();
		return count;
	} 
}
```
- ClientBean이 스프링 컨테이너의 정보를 갖고 있어 프로토타입 빈을 사용할 때마다 새로 요청하는 방법이다.
- 의존관계를 외부에서 주입받는 것이 아닌 직접 찾는 것을 **Dependency Lookup(DL)**이라고 한다.
- 스프링 컨테이너 전체를 주입받게 되면 스프링 컨테이너에 종속적인 코드가 되므로 좋지 못하다.

#### 문제 해결 : ObjectProvider (스프링 제공)
```java
static class ClientBean {
	@Autowired
  	private ObjectProvider<PrototypeBean> prototypeBeanProvider;
  	
    public int logic() {
      	PrototypeBean prototypeBean = prototypeBeanProvider.getObject();
      	prototypeBean.addCount();
      	int count = prototypeBean.getCount();
      	return count;
	}
}
```

#### 문제 해결 : Provider (라이브러리 추가 필요)
```java
static class ClientBean {
	@Autowired
  	private Provider<PrototypeBean> provider;
  	
    public int logic() {
      	PrototypeBean prototypeBean = provider.get();
      	prototypeBean.addCount();
      	int count = prototypeBean.getCount();
      	return count;
	}
}
```
- 자바 표준이므로 스프링이 아닌 다른 컨테이너에서도 사용할 수 있다.
- Provider.get을 호출하면 스프링 컨테이너를 통해 해당 빈을 찾아서 반환한다.(DL)

### 웹 스코프
- 웹 스코프는 웹 환경에서만 동작하며 스프링이 종료 시점까지 관리한다.

**웹 스코프 종류**
- request: HTTP 요청 하나가 들어오고 나갈 때 까지 유지되는 스코프, 각각의 HTTP 요청마다 별도의 빈 인스턴스가 생성되고, 관리된다.
- session: HTTP Session과 동일한 생명주기를 가지는 스코프
- application: 서블릿 컨텍스트(ServletContext)와 동일한 생명주기를 가지는 스코프
- websocket: 웹 소켓과 동일한 생명주기를 가지는 스코프

**예제**
```java
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MyLogger {

    private String uuid;
    private String requestURL;

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    public void log(String message){
        System.out.println("["+uuid+"]"+"["+requestURL+"]"+message);
    }

    @PostConstruct
    public void init(){
        uuid = UUID.randomUUID().toString();
        System.out.println("["+uuid+"] request scope bean create: "+ this);
    }

    @PreDestroy
    public void close(){
//        System.out.println("MyLogger.close");
        System.out.println("["+uuid+"] request scope bean close: "+ this);
    }
}
```
- MyLogger를 자동 주입하는 과정에서 초기 스프링 컨테이너 생성 시에는 HTTP request가 없으므로 주입을 할 수 없어서 오류를 발생

#### 해결 : ObjectProvider
```java
@Controller
@RequiredArgsConstructor
public class LogDemoController {

    private final LogDemoService logDemoService;
    private final ObjectProvider<MyLogger> myLoggerPrvider;

    @RequestMapping("log-demo")
    @ResponseBody
    public String logDemo(HttpServletRequest request){
        String requestURL = request.getRequestURI().toString();
		MyLogger myLogger = myLoggerProvider.getObject();
        System.out.println("myLogger = " + myLogger.getClass());

        myLogger.setRequestURL(requestURL);
        myLogger.log("controller test");
        logDemoService.logic("testID");

        return "OK";
    }
}
```
#### 해결 : Proxy
```java
@Controller
@RequiredArgsConstructor
public class LogDemoController {

    private final LogDemoService logDemoService;
    private final MyLogger myLogger;

    @RequestMapping("log-demo")
    @ResponseBody
    public String logDemo(HttpServletRequest request){
        String requestURL = request.getRequestURI().toString();

        System.out.println("myLogger = " + myLogger.getClass());

        myLogger.setRequestURL(requestURL);
        myLogger.log("controller test");
        logDemoService.logic("testID");

        return "OK";
    }
}
```
- proxyMode은 CGLIB 라이브러리로 MyLogger를 상속 받은 가짜 프록시 클래스를 만들어 두고 HTTP request와 상관 없이 가짜 프록시 클래스를 다른 빈에 미리 주입할 수 있다.
- 이 가짜 프록시 객체는 HTTP request가 실제로 오면 내부에서 진짜 빈을 요청하는 위임 로직이 들어있다.