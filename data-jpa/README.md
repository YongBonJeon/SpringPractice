## 공통 인터페이스
![](https://velog.velcdn.com/images/bon0057/post/5dcb6f64-0824-45c3-9d19-1666d6610f6f/image.png)
### 주요 메서드
- save(S) : 새로운 엔티티는 저장하고 이미 있는 엔티티는 병합한다.
- delete(T) : 엔티티 하나를 삭제한다. 내부에서 EntityManager.remove() 호출
- findById(ID) : 엔티티 하나를 조회한다. 내부에서 EntityManager.find() 호출
- getOne(ID) : 엔티티를 프록시로 조회한다. 내부에서 EntityManager.getReference() 호출
- findAll(...) : 모든 엔티티를 조회한다. 정렬(Sort)이나 페이징(Pageable) 조건을 파라미터로 제공할 수 있다.

## 쿼리 메소드 기능

### 메소드 이름으로 쿼리 생성
```java
public interface MemberRepository extends JpaRepository<Member, Long> {
     List<Member> findByUsernameAndAgeGreaterThan(String username, int age);
}
```
- 스프링 데이터 JPA는 메소드 이름을 분석해서 JPQL을 생성하고 실행
- 조회: find...By ,read...By ,query...By get...By,

    - [공식 문서](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query- methods.query-creation)
    - 예:) findHelloBy 처럼 ...에 식별하기 위한 내용(설명)이 들어가도 된다.
- COUNT: count...By 반환타입 long
- EXISTS: exists...By 반환타입 boolean
- 삭제: delete...By, remove...By 반환타입 long
- DISTINCT: findDistinct, findMemberDistinctBy
- LIMIT: findFirst3, findFirst, findTop, findTop3

### 메소드 이름으로 JPA NamedQuery 호출
#### JPA를 직접 사용해서 Named 쿼리 호출
```java
@Entity
@NamedQuery(
         name="Member.findByUsername",
         query="select m from Member m where m.username = :username")
public class Member {
	... 
}

public class MemberRepository {
     public List<Member> findByUsername(String username) {
         ...
         List<Member> resultList =
             em.createNamedQuery("Member.findByUsername", Member.class)
    } 
}
```
#### 스프링 데이터 JPA로 Named 쿼리 호출
```java
public interface MemberRepository extends JpaRepository<Member, Long> { //** 여기 선언한 Member 도메인 클래스
     List<Member> findByUsername(@Param("username") String username);
 }
 ```
- 스프링 데이터 JPA는 선언한 "도메인 클래스 + .(점) + 메서드 이름"으로 Named 쿼리를 찾아서 실행 - 만약 실행할 Named 쿼리가 없으면 메서드 이름으로 쿼리 생성 전략을 사용한다.

### @Query 어노테이션을 사용해서 레퍼지토리 인터페이스에 쿼리 직접 정의
```java
@Query("select m from Member m where m.username= :username and m.age = :age")
List<Member> findUser(@Param("username") String username, @Param("age") int age);

@Query("select m.username from Member m")
 List<String> findUsernameList();

 @Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) " +
         "from Member m join m.team t")
List<MemberDto> findMemberDto();
```

## 파라미터 바인딩
```java
@Query("select m from Member m where m.username = :name")
Member findMembers(@Param("name") String username);
     
@Query("select m from Member m where m.username in :names")
 List<Member> findByNames(@Param("names") List<String> names);
```

## 반환 타입
- 스프링 데이터 JPA는 유연한 반환 타입 지원
```java
List<Member> findByUsername(String name); //컬렉션 
Member findByUsername(String name); //단건
Optional<Member> findByUsername(String name); //단건 Optional
```
## 순수 JPA 페이징과 정렬
### JPA 페이징 예제
```java
public List<Member> findByPage(int age, int offset, int limit) {
     return em.createQuery("select m from Member m where m.age = :age order by m.username desc")
             .setParameter("age", age)
             .setFirstResult(offset)
             .setMaxResults(limit)
             .getResultList();
       }

public long totalCount(int age) {
    return em.createQuery("select count(m) from Member m where m.age = :age", Long.class)
            .setParameter("age", age)
            .getSingleResult();
}

@Test
public void paging() throws Exception {
	//given
	memberJpaRepository.save(new Member("member1", 10));
    memberJpaRepository.save(new Member("member2", 10));
    memberJpaRepository.save(new Member("member3", 10));
    memberJpaRepository.save(new Member("member4", 10));
    memberJpaRepository.save(new Member("member5", 10));
     
    int age = 10;
    int offset = 0;
    int limit = 3;
	
	//when
    List<Member> members = memberJpaRepository.findByPage(age, offset, limit);
    long totalCount = memberJpaRepository.totalCount(age);

	//페이지 계산 공식 적용...
	// totalPage = totalCount / size ... // 마지막 페이지 ...
	// 최초 페이지 ..
	
    //then
    assertThat(members.size()).isEqualTo(3);
    assertThat(totalCount).isEqualTo(5);
 }
```

## 스프링 데이터 JPA 페이징과 정렬
### 페이징과 정렬 파라미터
- org.springframework.data.domain.Sort : 정렬 기능
- org.springframework.data.domain.Pageable : 페이징 기능 (내부에 Sort 포함)

### 특별한 반환 타입
- org.springframework.data.domain.Page : 추가 count 쿼리 결과를 포함하는 페이징
- org.springframework.data.domain.Slice : 추가 count 쿼리 없이 다음 페이지만 확인 가능(내부적으로 limit + 1조회)
- List (자바 컬렉션): 추가 count 쿼리 없이 결과만 반환

```java
Page<Member> findByUsername(String name, Pageable pageable); //count 쿼리 사용 Slice<Member> findByUsername(String name, Pageable pageable); //count 쿼리 사용 안함 
List<Member> findByUsername(String name, Pageable pageable); //count 쿼리 사용 안함 
List<Member> findByUsername(String name, Sort sort);
```

### 스프링 데이터 페이징 예제
```java
public interface MemberRepository extends Repository<Member, Long> {
     Page<Member> findByAge(int age, Pageable pageable);
}

//페이징 조건과 정렬 조건 설정
@Test
public void page() throws Exception {
	//given
    memberRepository.save(new Member("member1", 10));
    memberRepository.save(new Member("member2", 10));
    memberRepository.save(new Member("member3", 10));
    memberRepository.save(new Member("member4", 10));
    memberRepository.save(new Member("member5", 10));
 
 	//when
    PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC,
"username"));
    Page<Member> page = memberRepository.findByAge(10, pageRequest);

	//then
	List<Member> content = page.getContent(); //조회된 데이터 	
    assertThat(content.size()).isEqualTo(3); //조회된 데이터 수 	
    assertThat(page.getTotalElements()).isEqualTo(5); //전체 데이터 수 
    assertThat(page.getNumber()).isEqualTo(0); //페이지 번호 
    assertThat(page.getTotalPages()).isEqualTo(2); //전체 페이지 번호 
    assertThat(page.isFirst()).isTrue(); //첫번째 항목인가? 
    assertThat(page.hasNext()).isTrue(); //다음 페이지가 있는가?
}
```
- 두 번째 파라미터로 받은 Pageable 은 인터페이스다. 따라서 실제 사용할 때는 해당 인터페이스를 구현한 org.springframework.data.domain.PageRequest 객체를 사용한다.
- PageRequest생성자의 첫 번째 파라미터에는 현재 페이지를, 두 번째 파라미터에는 조회할 데이터 수를 입력한다. 여기에 추가로 정렬 정보도 파라미터로 사용할 수 있다. 참고로 페이지는 0부터 시작한다.

```java
@Query(value = "select m from Member m",
        countQuery = "select count(m.username) from Member m")
Page<Member> findMemberAllCountBy(Pageable pageable);
```
- count 쿼리 분리 가능

```java
Page<Member> page = memberRepository.findByAge(10, pageRequest);
Page<MemberDto> dtoPage = page.map(m -> new MemberDto());
```
- 페이지를 유지하면서 엔티티를 DTO로 변환 가능

## 벌크성 수정 쿼리

### JPA 사용 벌크성  수정 쿼리
```java
public int bulkAgePlus(int age) {
     int resultCount = em.createQuery(
             "update Member m set m.age = m.age + 1" +
                     "where m.age >= :age")
             .setParameter("age", age)
             .executeUpdate();
     return resultCount;
}

@Test
public void bulkUpdate() throws Exception {
	
    //given
     memberJpaRepository.save(new Member("member1", 10));
     memberJpaRepository.save(new Member("member2", 19));
     memberJpaRepository.save(new Member("member3", 20));
     memberJpaRepository.save(new Member("member4", 21));
     memberJpaRepository.save(new Member("member5", 40));
	
    //when
    int resultCount = memberJpaRepository.bulkAgePlus(20);
	
    //then
    assertThat(resultCount).isEqualTo(3);
}
```
### 스프링 데이터 JPA를 사용한 벌크성 수정 쿼리
```java
@Modifying
@Query("update Member m set m.age = m.age + 1 where m.age >= :age")
int bulkAgePlus(@Param("age") int age); 

@Test
public void bulkUpdate() throws Exception {
	//given
    memberRepository.save(new Member("member1", 10));
    memberRepository.save(new Member("member2", 19));
    memberRepository.save(new Member("member3", 20));
    memberRepository.save(new Member("member4", 21));
    memberRepository.save(new Member("member5", 40));

	//when
    int resultCount = memberRepository.bulkAgePlus(20);

	//then
    assertThat(resultCount).isEqualTo(3);
} 
```
- 벌크성 수정, 삭제 쿼리는 @Modifying 어노테이션을 사용
- 벌크성 쿼리를 실행하고 나서 영속성 컨텍스트 초기화: @Modifying(clearAutomatically = true)
- 벌크 연산은 영속성 컨텍스트를 무시하고 실행하기 때문에, 영속성 컨텍스트와 DB의 엔티티 상태가 달라질 수 있음에 주의하자

## @EntityGraph
- member team은 지연로딩 관계이다. 따라서 다음과 같이 team의 데이터를 조회할 때 마다 쿼리가 실행된다. (N+1 문제 발생)
```java
@Test
public void findMemberLazy() throws Exception {
	//given
    //member1 -> teamA
    //member2 -> teamB
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    teamRepository.save(teamA);
    teamRepository.save(teamB);
     
    memberRepository.save(new Member("member1", 10, teamA));
    memberRepository.save(new Member("member2", 20, teamB));
     
    em.flush();
    em.clear();
	
    //when
    List<Member> members = memberRepository.findAll();

	//then (지연로딩으로 인해 N+1)문제 발생
    for (Member member : members) {
		member.getTeam().getName();
	} 
}
```
- 연관된 엔티티를 한번에 조회하려면 페치 조인이 필요하다.

### JPQL 페치 조인
```java
@Query("select m from Member m left join fetch m.team")
List<Member> findMemberFetchJoin();
```

- 스프링 데이터 JPA는 JPA가 제공하는 엔티티 그래프 기능을 편리하게 사용하게 도와준다. 이 기능을 사용하면 JPQL 없이 페치 조인을 사용할 수 있다. (JPQL + 엔티티 그래프도 가능)

### EntityGraph
```java
//공통 메서드 오버라이드
@Override
@EntityGraph(attributePaths = {"team"}) List<Member> findAll();

//JPQL + 엔티티 그래프 @EntityGraph(attributePaths = {"team"}) @Query("select m from Member m") List<Member> findMemberEntityGraph();

//메서드 이름으로 쿼리에서 특히 편리하다. @EntityGraph(attributePaths = {"team"})
List<Member> findByUsername(String username)
```
- 사실상 페치 조인(FETCH JOIN)의 간편 버전
- LEFT OUTER JOIN 사용

## JPA Hint & Lock

### 쿼리 힌트
```java
@QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
Member findReadOnlyByUsername(String username);

@Test
public void queryHint() throws Exception {
	//given
    memberRepository.save(new Member("member1", 10));
     
    em.flush();
    em.clear();

	//when
    Member member = memberRepository.findReadOnlyByUsername("member1");
    member.setUsername("member2");

	em.flush(); //Update Query 실행X 
}

@QueryHints(value = { @QueryHint(name = "org.hibernate.readOnly",
                                  value = "true")},
             forCounting = true)
Page<Member> findByUsername(String name, Pageable pageable);
```

### Lock
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<Member> findByUsername(String name);
```

## 사용자 정의 레퍼지토리 구현
- 스프링 데이터 JPA 리포지토리는 인터페이스만 정의하고 구현체는 스프링이 자동 생성
- 스프링 데이터 JPA가 제공하는 인터페이스를 직접 구현하면 구현해야 하는 기능이 너무 많음

### 사용자 정의 인터페이스
```java
 public interface MemberRepositoryCustom {
     List<Member> findMemberCustom();
}
```
### 사용자 정의 인터페이스 구현 클래스
```java
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {
	private final EntityManager em;
	@Override
     
	public List<Member> findMemberCustom() {
		return em.createQuery("select m from Member m")
                 .getResultList();
	} 
}
```
- 규칙: 리포지토리 인터페이스 이름 + Impl or 사용자 정의 인터페이스 이름 + Impl
- 스프링 데이터 JPA가 인식해서 스프링 빈으로 등록

### 사용자 정의 인터페이스 상속
```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
}
```

## Auditing
- 엔티티를 생성, 변경할 때 변경한 사람과 시간 추적

### 순수 JPA 사용
```java
@MappedSuperclass
@Getter
public class JpaBaseEntity {

	@Column(updatable = false)
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
      
	@PrePersist
    public void prePersist() {
		LocalDateTime now = LocalDateTime.now();
        createdDate = now;
        updatedDate = now;
	}
    @PreUpdate
    public void preUpdate() {
		updatedDate = LocalDateTime.now();
	}
}

public class Member extends JpaBaseEntity {}
 
@Test
public void jpaEventBaseEntity() throws Exception {

	//given
    Member member = new Member("member1");
    memberRepository.save(member); //@PrePersist
     
    Thread.sleep(100);
	member.setUsername("member2");
     
	em.flush(); //@PreUpdate
    em.clear();

	//when
    Member findMember = memberRepository.findById(member.getId()).get();

	//then
    System.out.println("findMember.createdDate = " + findMember.getCreatedDate());
	System.out.println("findMember.updatedDate = " + findMember.getUpdatedDate());
}
```

### 스프링 데이터 JPA 사용
- @EnableJpaAuditing -> 스프링 부트 설정 클래스에 적용
- @EntityListeners(AuditingEntityListener.class) -> 엔티티 적용
- @CreatedDate
- @LastModifiedDate
- @CreatedBy
- @LastModifiedBy

```java
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class BaseEntity {
	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime createdDate;
     
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
     
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;
    
    @LastModifiedBy
    private String lastModifiedBy;
}

```java
@EnableJpaAuditing
 @SpringBootApplication
 public class DataJpaApplication {
      public static void main(String[] args) {
           SpringApplication.run(DataJpaApplication.class, args);
}
      @Bean
      public AuditorAware<String> auditorProvider() {
           return () -> Optional.of(UUID.randomUUID().toString());
      }
}
```
- 등록자, 수정자를 처리해주는 AuditorAware 스프링 빈 등록
- 실무에서는 세션 정보나, 스프링 시큐리티 로그인 정보에서 ID를 받음

## Web 확장
### 도메인 클래스 컨버터
- HTTP 파라미터로 넘어온 엔티티의 아이디로 엔티티 객체를 찾아서 바인딩

#### 도메인 클래스 컨버터 사용 전
```java
@RestController
@RequiredArgsConstructor
public class MemberController {
	private final MemberRepository memberRepository;
	@GetMapping("/members/{id}")
	public String findMember(@PathVariable("id") Long id) {
		Member member = memberRepository.findById(id).get();
		return member.getUsername();
	}
}
```
#### 도메인 클래스 컨버터 사용 후
```java
@RestController
@RequiredArgsConstructor
public class MemberController {
	private final MemberRepository memberRepository;
	
    @GetMapping("/members/{id}")
	public String findMember(@PathVariable("id") Member member) {
		return member.getUsername();
	}
}
```
- HTTP 요청은 회원 id를 받지만 도메인 클래스 컨버터가 중간에 동작해서 회원 엔티티 객체를 반환
- 도메인 클래스 컨버터도 레퍼지토리를 사용해서 엔티티를 찾음
- 주의사항 : 도메인 클래스 컨버터로 엔티티를 파라미터로 받으면, 이 엔티티는 단순 조회용으로만 사용해야 한다. (트랜잭션이 없는 범위에서 엔티티를 조회했으므로, 엔티티를 변경해도 DB에 반영되지 않는다.)

### 페이징과 정렬
- 스프링 데이터가 제공하는 페이징과 정렬 기능을 스프링 MVC에서 편리하게 사용할 수 있다.

```java
@GetMapping("/members")
public Page<Member> list(Pageable pageable) {
	Page<Member> page = memberRepository.findAll(pageable);
	return page;
}
```

#### 요청 파라미터
- 예) /members?page=0&size=3&sort=id,desc&sort=username,desc
- page: 현재 페이지, **0부터 시작한다.**
- size: 한 페이지에 노출할 데이터 건수
- sort: 정렬 조건을 정의한다. 예) 정렬 속성,정렬 속성...(ASC | DESC), 정렬 방향을 변경하고 싶으면 sort 파라 미터 추가 ( asc 생략 가능

### Page 내용 -> DTO
- 엔티티를 API로 노출하면 다양한 문제가 발생한다. 그래서 엔티티를 꼭 DTO로 변환해서 반환해야 한다.
- Page는 map() 을 지원해서 내부 데이터를 다른 것으로 변경할 수 있다.
```java
	public Page<MemberDto> list(Pageable pageable) {
     	Page<Member> page = memberRepository.findAll(pageable);
     	Page<MemberDto> pageDto = page.map(MemberDto::new);
     	return pageDto;
	}
```