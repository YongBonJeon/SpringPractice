## JDBC
### JDBC 등장 이유
- 클라이언트가 애플리케이션 서버를 통해 데이터를 저장하거나 조회하면, 애플리케이션 서버는 DB를 사용한다.
  ![](https://velog.velcdn.com/images/bon0057/post/d07d0d04-25b0-4b66-a958-3a2275e101f6/image.png)
- **각 데이터베이스마다 커넥션 연결 방법, SQL 전달 방법, 결과 응답 방법이 모두 다르다는 것이 문제**이다.
- 이를 해결하기 위해 **JDBC라는 자바 표준**이 등장한다.

### JDBC 표준 인터페이스
![](https://velog.velcdn.com/images/bon0057/post/c37d3d66-748e-4e66-8c80-25ee071cde5a/image.png)
- **JDBC는 자바에서 데이터베이스에 접속할 수 있도록 하는 자바 API**이다. JDBC는 데이터베이스에서 자료를 쿼리하거나 업데이트하는 방법을 제공한다.
- 자바는 이렇게 표준 인터페이스를 정의해두었고 각각의 DB 회사에서 이를 구현해서 라이브러리로 제공하는데, 이것이 JDBC 드라이버이다.

> JDBC의 등장으로 데이터베이스 변경 시에 애플리케이션의 DB 사용 코드를 변경해야 하는 문제와 개발자가 데이터베이스마다 사용법을 학습해야 한다는 문제를 해결했다. 하지만 JDBC 코드는 변경하지 않아도 되지만 SQL은 여전히 해당 데이터베이스에 맞도록 변경해야 한다.

### SQL Mapper
![](https://velog.velcdn.com/images/bon0057/post/1d881017-c53a-477f-b689-809daafc85f7/image.png)

- SQL 응답 결과를 객체로 편리하게 변환해주고 JDBC의 반복 코드를 제거해주는 장점이 있다.
- 개발자가 SQL을 직접 작성해야 한다.
- MyBatis, JdbcTemplate

### ORM 기술
![](https://velog.velcdn.com/images/bon0057/post/19442e0e-8c74-4c06-8bfa-d2e3ca8fa0b6/image.png)
- ORM은 객체를 관계형 데이터베이스 테이블과 매핑해주는 기술이다. 이 덕분에 개발자는 반복적인 SQL을 직접 작성하지 않고, ORM 기술이 개발자 대신에 SQL을 동적으로 만들어 실행해준다. 추가로 각각의 DB마다 다른 SQL을 사용하는 문제도 중간에서 해결해준다. **JPA는 자바 진영의 ORM 표준 인터페이스**이고, 이것을 구현한 것으로 하비어네이트와 이클립스 링크 등의 구현 기술이 있다.

## CRUD - 순수 JDBC
### 연결
```java
public static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            log.info("get connection={}, class={}", connection, connection.getClass());
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
```
- 데이터베이스에 연결하려면 JDBC가 제공하는 DriverManager.getConnection(..)을 사용하면 된다. 라이브러리에 있는 데이터베이스 드라이버를 찾아서 해당 드라이버가 제공하는 커넥션을 반환해준다.
- 테스트 실행 결과를 보면 class=class org.h2.jdbc.JdbcConnection 부분을 확인할 수 있다. 이것이 바로 H2 데이터베이스 드라이버가 제공하는 H2 전용 커넥션이다. 이 커넥션은 JDBC 표준 커넥션 인터페이스인 java.sql.Connection 인터페이스를 구현하고 있다.


![](https://velog.velcdn.com/images/bon0057/post/e7748a13-8c4e-432d-9a5b-5c35da23d262/image.png)
1. 커넥션이 필요하면 DriverManager.getConnection 호출
2. DriverManager는 라이브러리에 등록된 드라이버 목록을 자동으로 인식한다. 이 드라이버들에게 정보를 넘겨 커넥션을 획득할 수 있는지 확인한다.
3. 현재 등록한 URL이 jdbc:h2로 시작하는데 이것이 h2 데이터베이스에 접근하기 위한 규칙이기에 h2 드라이버는 본인이 처리할 수 있으므로 실제 데이터베이스에 연결해서 커넥션을 획득한다. 다른 드라이버가 먼저 접근해도 처리할 수 없다면 다음 드라이버에게 순서가 넘어간다.

### 등록
**Member 도메인**
```java
@Data
public class Member {
    private String memberId;
    private int money;

    public Member() {
    }

    public Member(String memberId, int money) {
        this.memberId = memberId;
        this.money = money;
    }
}
```
- H2 데이터베이스는 현재 Member 테이블이 존재하지 않으므로 SQL을 통해 직접 만들어주어야 한다.
```sql
	drop table member if exists cascade;
    create table member (
        member_id varchar(10),
        money integer not null default 0,
        primary key (member_id)
	);
```

```java
	public Member save(Member member) throws SQLException {
        String sql = "insert into member(member_id, money) values (?, ?)";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = DBConnectionUtil.getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, member.getMemberId());
            pstmt.setInt(2, member.getMoney());
            pstmt.executeUpdate();
            return member;
        } catch (SQLException e) {
            log.error("db error", e);
            throw e;
        } finally {
            close(con, pstmt, null);
        }
    }
    
    private void close(Connection con, Statement stmt, ResultSet rs) {

        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.info("error", e);
            }

        }

        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.info("error", e);
            }

        }

        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                log.info("error", e);
            }

        }
    }
```
- getConnection : 데이터베이스 커넥션 획득
- sql : 데이터베이스에 전달할 SQL 정의
- con.prepareStatement(sql) : 데이터베이스에 전달할 SQL과 데이터 준비
- pstmt.executeUpdate() : 준비된 sql을 커넥션을 통해 실제 DB에 전달. 반환값은 영향받은 DB row 수이다.

### 조회
```java
	public Member findById(String memberId) throws SQLException {
        String sql = "select * from member where member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = DBConnectionUtil.getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                Member member = new Member();
                member.setMemberId(rs.getString("member_id"));
                member.setMoney(rs.getInt("money"));
                return member;
            } else {
                throw new NoSuchElementException("member not found memberId=" + memberId);
            }

        } catch (SQLException e) {
            log.error("db error", e);
            throw e;
        } finally {
            close(con, pstmt, rs);
        }
    }
```
- rs = pstmt.executeQuery() : 데이터를 변경할 때는 executeUpdate()를 사용하지만, 데이터를 조회할 때는 executeQuery()를 사용한다.

#### resultSet
![](https://velog.velcdn.com/images/bon0057/post/142a7e65-f575-450f-8d54-457a52ef99dd/image.png)

#### 조회 테스트
```java
	@Test
	void crud() throws SQLException {
		//save
		Member member = new Member("memberV0", 10000);
		repository.save(member);
		
        //findById
		Member findMember = repository.findById(member.getMemberId());
		log.info("findMember={}", findMember);
		assertThat(findMember).isEqualTo(member);
	}
```
- log.info() 에서 member 객체의 참조 값이 아닌 실제 데이터가 보여주는 이유는 @Data가 toString()을 적절히 오버라이딩하기 때문이다.
- isEqulTo() : 이 결과가 참인 이유는 @Data가 해당 객체의 모든 필드를 사용하도록 eqauls()를 오버라이딩하기 때문이다.

### 수정, 삭제
```java
	public void update(String memberId, int money) throws SQLException {
        String sql = "update member set money=? where member_id=?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = DBConnectionUtil.getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, money);
            pstmt.setString(2, memberId);
            int resultSize = pstmt.executeUpdate();
            log.info("resultSize = {}", resultSize);
        } catch (SQLException e) {
            log.error("db error", e);
            throw e;
        } finally {
            close(con, pstmt, null);
        }
    }

    public void delete(String memberId) throws SQLException {
        String sql = "delete from member where member_id=?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = DBConnectionUtil.getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("db error", e);
            throw e;
        } finally {
            close(con, pstmt, null);
        }
    }
```

## 기존 데이터베이스 커넥션 획득 방법
![](https://velog.velcdn.com/images/bon0057/post/e3344888-45db-42c2-98ac-753a23f55928/image.png)
- 애플리케이션 로직은 DB 드라이버를 통해 커넥션 조회
- DB 드라이버는 DB와 TCP/IP 커넥션 연결
- DB 드라이버는 DB에 ID, Password, 부가 정보 전달
- DB는 내부 인증 완료 후 DB 세션 생성
- DB가 커넥션 생성이 완료되었다는 응답을 보냄
- DB 드라이버가 커넥션 객체를 생성해 클라이언트에게 반환

> 애플리케이션 사용 시에 매번 커넥션 생성, SQL 생성하는 것은 꽤 큰 오버헤드이다.
이런 문제를 해결하기 위해 **커넥션 풀**을 사용한다.

## 커넥션 풀

- ![](https://velog.velcdn.com/images/bon0057/post/8338120b-c772-40b2-b865-615c56efa369/image.png)
- 애플리케이션을 시작하는 시점에 커넥션 풀은 필요한 만큼 미리 커넥션을 확보해 풀에 보관한다.
  ![](https://velog.velcdn.com/images/bon0057/post/d9cd1608-9d48-4686-a0e8-dc1776885eff/image.png)
- 커넥션 풀에 들어 있는 커넥션은 TCP/IP로 DB와 커넥션이 연결되어 있는 상태이기 때문에 언제든지 SQL을 DB에 전달할 수 있다.
- 애플리케이션 로직은 커넥션 풀에서 받은 커넥션을 사용해서 SQL을 데이터베이스에 전달하고 그 결과를 받아서 처리한다. 커넥션을 사용하고 나면 커넥션을 종료하는 것이 아니라, 다음에 다시 사용할 수 있도록 그대로 커넥션 풀에 반환하면 된다.
- 커넥션 풀 숫자는 서비스의 특징, 서버 스펙, DB 서버 스펙 등에 따라 다르기에 성능 테스트를 통해 정해야한다.
- 커넥션 풀이 주는 이점이 많기 때문에 실무에서는 항상 기본으로 사용한다.
- 커넥션 풀은 개념적으로 단순해서 직접 구현할 수도 있지만, 사용도 편리하고 성능도 뛰어난 오픈소스 **hikariCP**를 주로 사용한다.

## Data Source
![](https://velog.velcdn.com/images/bon0057/post/b0eac543-4600-4321-b0f2-1a0d3856273e/image.png)
- 커넥션을 얻는 방법은 앞서 설명했듯이 DriverManager, 커넥션 풀 등이 있는데 애플리케이션 로직에서 커넥션을 얻는 방법을 수정할 때 코드를 수정해야 하므로 커넥션을 얻는 방법을 **추상화**해야한다.
  ![](https://velog.velcdn.com/images/bon0057/post/5e103036-d2d4-4ed9-b617-7b1e407e3247/image.png)
- 자바는 이를 위해 javax.sql.DataSource라는 인터페이스를 제공한다. 이 인터페이스의 핵심 기능은 커넥션 조회 하나이다.
```java
	public interface DataSource {
    	Connection getConnection() throws SQLException;
	}
```

### DriverManager
```java
@Test
    void driverManager() throws SQLException {
        Connection connection1 = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        Connection connection2 = DriverManager.getConnection(URL, USERNAME, PASSWORD);

        log.info("connection={}, class={}", connection1, connection1.getClass());
        log.info("connection={}, class={}", connection2, connection2.getClass());
    }

    @Test
    void dataSourceDriverManager() throws SQLException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        useDataSource(dataSource);
    }

    private void useDataSource(DataSource dataSource) throws SQLException {
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();

        log.info("connection={}, class={}", connection1, connection1.getClass());
        log.info("connection={}, class={}", connection2, connection2.getClass());
    }
```
- DriverManagerDataSource와 DriverManager은 비슷하지만 커넥션을 획득하는 방법에서 차이가 있다. DriverManager는 getConnection()을 호출할 때 매번 파라미터를 전달해야 하지만 DataSource를 이용하는 방식에선 처음 객체를 생성할 때만 필요한 파라미터를 넘겨두고, 그 다음부터 getConnection()을 호출할 때에는 파라미터를 전달하지 않아도 된다.
- 레퍼지토리가 DataSource만을 의존하고 **URL, Password 등 이런 속성들에 의존하지 않는다**.

### 커넥션 풀
```java
    @Test
    void dataSourceConnectionPool() throws SQLException, InterruptedException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setMaximumPoolSize(10);
        dataSource.setPoolName("MyPool");

        useDataSource(dataSource);
        Thread.sleep(1000);
    }
    
    private void useDataSource(DataSource dataSource) throws SQLException {
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();

        log.info("connection={}, class={}", connection1, connection1.getClass());
        log.info("connection={}, class={}", connection2, connection2.getClass());
    }
```
- HikariCP 커넥션 풀을 사용한다. HikariDataSource는 DataSource 인터페이스를 구현하고 있다.
- 커넥션 풀 최대 사이즈를 10으로 지정한다. 커넥션 풀에서 커넥션을 생성하는 작업은 별도의 쓰레드에서 작동한다. 그렇기에 테스트가 먼저 종료되어 버리므로 sleep을 통해 생성을 확인한다.

## CRUD - dataSource

```java
@Slf4j
public class MemberRepositoryV1 {

    private final DataSource dataSource;

    public MemberRepositoryV1(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Member save(Member member) throws SQLException {
        ...
    }

    public Member findById(String memberId) throws SQLException {
        ...
    }

    public void update(String memberId, int money) throws SQLException {
        ...
    }

    public void delete(String memberId) throws SQLException {
        ...
    }

    private void close(Connection con, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        JdbcUtils.closeConnection(con);
    }

    private Connection getConnection() throws SQLException {
        Connection con = dataSource.getConnection();
        log.info("get Connection={}, class ={}", con, con.getClass());
        return con;
    }
```
- DataSource는 **외부에서 주입 받아서 사용**한다. DataSource는 표준 인터페이스이기 때문에 변경되어도 괜찮다.
- 스프링은 JDBC를 편하게 다룰 수 있는 JdbcUtils라는 편의 메서드를 제공하고 이를 이용해 커넥션을 편리하게 닫을 수 있다.

> DriverManagerDataSource -> HikariDataSource로 변경해도 애플리케이션 코드를 변경하지 않아도 된다. DataSource **인터페이스**에만 의존하기 때문이다. (Dependency Injection + Open-closed principle)

## 트랜잭션
- 데이터베이스에서 트랜잭션은 하나의 거래를 안전하게 처리하도록 보장해주는 것을 뜻한다. 트랜잭션에서 모든 작업이 성공해서 데이터베이스에 정상 반영하는 것은 커밋(Commit), 작업 중 하나라도 실패해서 거래 이전으로 되돌리는 것을 롤백(Rollback)이라 한다.

### ACID
- 트랜잭션은 원자성(Atomicity), 일관성(Consistency), 격리성(Isolation), 지속성(Durablity)을 보장해야 한다.
- 원자성 : 트랜잭션 내에서 실행한 작업들은 마치 하나의 작업인 것처럼 모두 성공하거나 실패해야 한다.
- 일관성 : 모든 트랜잭션은 일관성 있는 데이터베이스 상태를 유지해야 한다. (ex. 무결성 제약 조건 항상 만족)
- 격리성 : 동시에 실행되는 트랜잭션들이 서로에게 영향을 미치지 않도록 격리한다. (같은 데이터 수정 X)
- 지속성 : 트랜잭션을 성공적으로 끝내면 그 결과가 항상 기록되어야 한다.

### 데이터베이스 연결 구조
![](https://velog.velcdn.com/images/bon0057/post/1a0e62eb-6911-45c3-8b86-f0577086c6cc/image.png)
- 사용자는 WAS나 DB 접근 툴 같은 클라이언트를 사용해서 데이터베이스 서버를 접근할 수 있다. 클라이언트는 데이터 서버에 연결을 요청하고 커넥션을 맺게 된다. 이때 데이터베이스 서버는 내부에 **세션**이라는 것을 만든다. 해당 **커넥션을 통한 요청들은 이 세션을 통해서 실행**된다 .
- 커넥션 풀이 여러 개의 커넥션을 요청하면 세션도 여러 개 만들어진다.

### 커밋 방식
- 자동 커밋으로 설정하면 쿼리 실행 직후에 자동으로 커밋이 실행되고 커밋이나 롤백을 따로 호출하지 않아도 되지만 원하는 트랜잭션 기능을 제대로 사용할 수 없다. 보통 자동 커밋 모드가 디폴트이므로 **수동 커밋 모드로 설정하는 것이 트랜잭션을 시작**한다고 표현할 수 있다.
- 커밋을 호출하기 전까지는 임시로 데이터를 저장하는 것이다. 해당 트랜잭션을 시작한 세션(사용자)에게만 변경 데이터가 보이고 다른 세션(사용자)에게는 변경 데이터가 보이지 않는다.

### 트랜잭션 예제 - 계좌이체
![](https://velog.velcdn.com/images/bon0057/post/7db2a387-fe8f-475c-ace6-7e2ae5ac42bb/image.png)
- 계좌이체를 실행하는 도중에 SQL에 문제가 생겨 memberA의 돈은 줄었지만 memberB의 돈이 증가하지 않은 문제 상황이다. 이렇게 중간에 문제가 생겼을 때는 커밋을 호출하면 안되고 **롤백을 호출**해서 데이터를 트랜잭션 시작 전으로 원복해야 한다.

## DB 락
- 세션1이 트랜잭션을 시작하고 데이터를 수정하는 동안 세션2에서 동시에 같은 데이터를 수정하면 안된다. 이런 문제를 방지하기 위해 커밋이나 롤백전, 즉 한 세션의 트랜잭션 동안 다른 세션에서 해당 데이터를 수정할 수 없게 막아야 한다.
  ![](https://velog.velcdn.com/images/bon0057/post/d4f2b12e-e6fd-4fd3-89e6-36c450b44ef5/image.png)

- 한 세션이 트랜잭션을 시작하면 해당 데이터의 락을 가져가게 되고 커밋이나 롤백을 하기 전까지 락을 갖고 있는다. 해당 데이터에 다른 세션이 접근하게 되면 락을 얻기 위해 대기한다.
- DB 락 대기도 무한정 하는 것이 아닌 락 타임아웃 시간을 설정할 수 있다.

### DB 락 - 조회
- 일반적으로 데이터 조회시에는 락을 획득하지 않지만 특별한 경우에 락을 획득하고 싶으면 **select for update** 구문을 사용하여 데이터를 조회한다면 DB 락을 획득할 수 있다.


## 트랜잭션 적용 예제
### 트랜잭션 미적용
```java
	@RequiredArgsConstructor
	public class MemberServiceV1 {

    	private final MemberRepositoryV1 memberRepository;

    	public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        	Member fromMember = memberRepository.findById(fromId);
        	Member toMember = memberRepository.findById(toId);

        	memberRepository.update(fromId, fromMember.getMoney() - money);
        	validation(toMember);
        	memberRepository.update(toId, toMember.getMoney() + money);
    	}

    	private static void validation(Member toMember) {
        	if (toMember.getMemberId().equals("ex")) {
            	throw new IllegalStateException("이체중 예외 발생");
        	}
    	}
	}
    
	@Test
    @DisplayName("정상 이체")
    void accountTransfer() throws SQLException {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);

        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);

        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(8000);
        assertThat(findMemberB.getMoney()).isEqualTo(12000);
    }

    @Test
    @DisplayName("이체중 예외 발생")
    void accountTransferEx() throws SQLException {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEx = new Member(MEMBER_EX, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);

        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberEx.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(8000);
        assertThat(findMemberB.getMoney()).isEqualTo(10000);
    }
```
- 현재 계좌이체 중 문제가 발생하면 memberA의 계좌에서 돈이 줄고 memberB의 계좌는 그대로인 문제 상황이다.

- **트랜잭션은 비즈니스 로직이 있는 서비스 계층에서 시작**해야 한다. 비즈니스 로직이 잘못되면 해당 비즈니스 로직으로 인해 문제가 되는 부분을 함께 롤백해야 하기 때문이다. 애플리케이션에서 DB 트랜잭션을 사용하기 위해 **같은 커넥션**을 유지해야한다.

### 트랜잭션 적용
**Repository**
```java
package hello.jdbc.repository;

import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.NoSuchElementException;

@Slf4j
public class MemberRepositoryV2 {


    private final DataSource dataSource;

    public MemberRepositoryV2(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Member findById(String memberId) throws SQLException {
        String sql = "select * from member where member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                Member member = new Member();
                member.setMemberId(rs.getString("member_id"));
                member.setMoney(rs.getInt("money"));
                return member;
            } else {
                throw new NoSuchElementException("member not found memberId=" + memberId);
            }

        } catch (SQLException e) {
            log.error("db error", e);
            throw e;
        } finally {
            close(con, pstmt, rs);
        }
    }

    public Member findById(Connection con, String memberId) throws SQLException {
        String sql = "select * from member where member_id = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                Member member = new Member();
                member.setMemberId(rs.getString("member_id"));
                member.setMoney(rs.getInt("money"));
                return member;
            } else {
                throw new NoSuchElementException("member not found memberId=" + memberId);
            }

        } catch (SQLException e) {
            log.error("db error", e);
            throw e;
        } finally {
            JdbcUtils.closeResultSet(rs);
            JdbcUtils.closeStatement(pstmt);
            //JdbcUtils.closeConnection(con);
        }
    }

    public void update(String memberId, int money) throws SQLException {
        String sql = "update member set money=? where member_id=?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, money);
            pstmt.setString(2, memberId);
            int resultSize = pstmt.executeUpdate();
            log.info("resultSize = {}", resultSize);
        } catch (SQLException e) {
            log.error("db error", e);
            throw e;
        } finally {
            close(con, pstmt, null);
        }
    }

    public void update(Connection con, String memberId, int money) throws SQLException {
        String sql = "update member set money=? where member_id=?";

        PreparedStatement pstmt = null;

        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, money);
            pstmt.setString(2, memberId);
            int resultSize = pstmt.executeUpdate();
            log.info("resultSize = {}", resultSize);
        } catch (SQLException e) {
            log.error("db error", e);
            throw e;
        } finally {
            JdbcUtils.closeStatement(pstmt);
            //JdbcUtils.closeConnection(con);
        }
    }

    private void close(Connection con, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        JdbcUtils.closeConnection(con);
    }

    private Connection getConnection() throws SQLException {
        Connection con = dataSource.getConnection();
        log.info("get Connection={}, class ={}", con, con.getClass());
        return con;
    }

}
```
- **트랜잭션을 위해 같은 커넥션을 유지해야 하기 때문에 repository 코드에서 매번 새롭게 커넥션을 받아오면 안된다. service 코드에서 주는 커넥션을 repository에서 계속 사용하고 그 커넥션을 닫아서는 안된다. **

**Service**
```java
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV2 {

    private final DataSource dataSource;
    private final MemberRepositoryV2 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {

        Connection con = dataSource.getConnection();
        try {
            // 트랜잭션 시작
            con.setAutoCommit(false);
            bizLogic(con, fromId, toId, money);
            con.commit();
        } catch (Exception e) {
            con.rollback();
            throw new IllegalStateException(e);
        } finally {
            release(con);
        }


    }

    private void bizLogic(Connection con, String fromId, String toId, int money) throws SQLException {
        Member fromMember = memberRepository.findById(con, fromId);
        Member toMember = memberRepository.findById(con, toId);

        memberRepository.update(fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private static void release(Connection con) {
        if (con != null) {
            try {
                con.setAutoCommit(true);
                con.close();
            } catch (Exception e) {
                log.info("error", e);
            }
        }
    }

    private static void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
```
- **트랜잭션을 시작하려면 커넥션이 필요**하다.
- con.setAutoCommit(false) : 트랜잭션 시작
- bizLogic(con, fromId, toId, money) : 트랜잭션이 시작된 커넥션을 전달하면서 비즈니스 로직 수행
- con.commit() : 트랜잭션 커밋
- con.rollback() : 비즈니스 로직 수행 도중에 예외가 발생하면 트랜잭션 롤백
- release(con) : 커넥션 종료(반납)

### 테스트
```java
class MemberServiceV2Test {

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    private MemberRepositoryV2 memberRepository;
    private MemberServiceV2 memberService;

    @BeforeEach
    void before() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        memberRepository = new MemberRepositoryV2(dataSource);
        memberService = new MemberServiceV2(dataSource, memberRepository);
    }

    @AfterEach
    void after() throws SQLException {
        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);
    }

    @Test
    @DisplayName("정상 이체")
    void accountTransfer() throws SQLException {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);

        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);

        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(8000);
        assertThat(findMemberB.getMoney()).isEqualTo(12000);
    }

    @Test
    @DisplayName("이체중 예외 발생")
    void accountTransferEx() throws SQLException {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEx = new Member(MEMBER_EX, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);

        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberEx.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(10000);
        assertThat(findMemberB.getMoney()).isEqualTo(10000);
    }
}
```
> 트랜잭션 적용은 되었으나 **서비스 계층의 코드가 매우 지저분하고 많은 의존성을 갖게 되는 문제**가 있다.

## 순수한 서비스 계층
- 프레젠테이션 계층, 데이터 접근 계층과 달리 서비스 계층은 특정 기술에 종속적이지 않게 개발해야 한다.
- 서비스 계층이 특정 기술에 종속되지 않기 때문에 비즈니스 로직을 유지보수 하기도 쉽고, 테스트 하기도 쉽다.
- 현재 애플리케이션의 문제점
    - 트랜잭션 문제
    - 예외 누수 문제
    - JDBC 반복 문제

### 트랜잭션 문제
- 트랜잭션을 적용하기 위해 JDBC 구현 기술이 서비스 계층에 누수되었다.
- 서비스 계층은 순수해야 하며 특정 기술에 종속되지 않아야 한다.
- 같은 트랜잭션을 유지하기 커넥션을 파라미터로 넘겨야 한다.
- 트랜잭션 적용 코드(try, catch, ..) 반복이 많다.

### 예외 누수 문제
- SQLException은 체크 예외이기 때문에 데이터 접근 계층을 호출한 서비스 계층에서 해당 예외를 잡아 처리하거나 명시적으로 throws를 통해서 다시 밖으로 던져야 한다.
- SQLException은 JDBC 전용 기술이다. JPA나 다른 데이터 접근 기술을 사용하면, 그에 맞는 예외로 변경해야 하고 결국 서비스 계층 코드도 수정해야 한다.

### JDBC 반복 문제
- 현재 유사한 코드의 반복이 너무 많다. try, catch, finally, 커넥션 연결, SQL 세팅 등

> 현재 서비스 계층은 트랜잭션을 사용하기 위해 **JDBC 기술에 의존**하고 있다. 이를 탈피하기 위해 트랜잭션을 추상화한다.

## 트랜잭션 추상화
- 데이터 접근 계층의 구현 기술마다 트랜잭션을 사용하는 방법이 다르기에 트랜잭션을 추상화해야 한다.
    - JDBC : con.setAutoCommit(false)


    - JPA : transaction.begin()

![](https://velog.velcdn.com/images/bon0057/post/cc5feffe-2269-47a4-b10e-17241363f511/image.png)


### 트랜잭션 추상화 인터페이스
![](https://velog.velcdn.com/images/bon0057/post/c588f251-268f-45a2-9686-5e4f2a2f480a/image.png)
```java
	public interface TxManager {
		begin();
        commit();
      	rollback();
	}
```


- 이 인터페이스를 기반으로 JDBC, JPA 등을 이용한 각각 구현체를 만든다.
  ![](https://velog.velcdn.com/images/bon0057/post/e8f7e195-7f56-430a-bf93-bd878c383bd6/image.png)
- 서비스는 특정 트랜잭션 기술을 직접 의존하는 것이 아니라 TxManager 인터페이스에 의존한다. 원하는 구현체를 DI(Dependency Injection)를 통해서 주입하여 사용하면 된다.

### 스프링의 트랜잭션 추상화
- 스프링은 트랜잭션 추상화 기술을 제공하고 각 데이터 접근 기술에 따른 트랜잭션 구현체도 대부분 만들어두어서 사용하기만 하면 된다.
  ![](https://velog.velcdn.com/images/bon0057/post/2f6cc3a4-8eeb-4b6b-a7a1-022f98ad5498/image.png)

#### PlatformTransactionManager 인터페이스
```java
	public interface PlatformTransactionManager extends TransactionManager {
		TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException;
      	void commit(TransactionStatus status) throws TransactionException;
      	void rollback(TransactionStatus status) throws TransactionException;
	}
```
- getTransaction() : 트랜잭션 시작
- commit() : 트랜잭션 커밋
- rollback() : 트랜잭션 롤백

### 스프링의 트랜잭션 동기화
- 트랜잭션을 유지하려면 트랜잭션의 시작부터 끝까지 같은 데이터베이스 커넥션을 유지해야 한다. 기존에는 커넥션을 동기화하기 위해 서비스 계층에서 데이터 접근 계층으로 파라미터로 커넥션을 전달하는 방식을 사용했다.
- **스프링은 트랜잭션 동기화 매니저를 제공**한다. 이것은 쓰레드 로컬을 이용해 커넥션을 동기화해준다. 트랜잭션 매니저는 내부에서 이를 사용한다. 더이상 같은 커넥션 유지를 위해 파라미터로 커넥션을 전달하지 않아도 된다.
  ![](https://velog.velcdn.com/images/bon0057/post/26d90c13-a52f-4c02-a088-b719257bd6fa/image.png)

- 트랜잭션을 시작하려면 커넥션이 필요. 트랜잭션 매니저는 데이터소스를 통해 커넥션을 만들고 트랜잭션 시작
- 트랜잭션 매니저는 커넥션을 트랜잭션 동기화 매니저에 보관
- 데이터 접근 계층은 트랜잭션 동기화 매니저에서 커넥션을 꺼내서 사용 (파라미터로 커넥션 전달 X)
- 트랜잭션이 종료되면 트랜잭션 매니저는 동기화 매니저에 보관된 커넥션을 통해 트랜잭션 종료, 커넥션도 종료

### 트랜잭션 매니저 예제
#### 데이터 접근 계층
```java
@Slf4j
public class MemberRepositoryV3 {

    private final DataSource dataSource;

    public MemberRepositoryV3(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Member save(Member member) throws SQLException {
        ...
    }

    public Member findById(String memberId) throws SQLException {
        ...
    }

    public void update(String memberId, int money) throws SQLException {
        ...
    }

    public void delete(String memberId) throws SQLException {
        ...
    }

    private void close(Connection con, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        DataSourceUtils.releaseConnection(con, dataSource);
    }

    private Connection getConnection() throws SQLException {

        Connection con = DataSourceUtils.getConnection(dataSource);
        //Connection con = dataSource.getConnection();
        log.info("get Connection={}, class ={}", con, con.getClass());
        return con;
    }

}
```
- ~~DataSource.getConnection()~~
- DataSourceUtils.getConnection() : **트랜잭션 동기화 매니저가 관리하는 커넥션이 있으면 해당 커넥션을 반환**한다. 커넥션이 없는 경우 새로운 커넥션을 생성해서 반환한다.
- DataSourceUtils.releaseConnection() : 기존처럼 con.close()를 사용하면 커넥션이 유지되지 않는 문제가 발생한다. releaseConnection()을 사용하면 커넥션을 바로 닫는 것이 아닌 동기화된 커넥션은 그대로 유지, 동기화된 커넥션이 없는 경우 커넥션을 닫는다.

#### 서비스 계층
```java
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV3_1 {
    private final PlatformTransactionManager transactionManager;
    private final MemberRepositoryV3 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {

        //트랜잭션 시작
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            bizLogic(fromId, toId, money);
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw new IllegalStateException(e);
        }
    }

    private void bizLogic(String fromId, String toId, int money) throws SQLException {
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private static void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
```
- private final PlatformTransactionManager transactionManager : 트랜잭션 매니저를 주입받는다.
- transactionManager.getTransaction() : 트랜잭션 시작, TransactionStatus status를 반환한다. 이는 커밋, 롤백에서 사용된다.

#### 테스트
```java
class MemberServiceV3_1Test {

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    private MemberRepositoryV3 memberRepository;
    private MemberServiceV3_1 memberService;

    @BeforeEach
    void before() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        memberRepository = new MemberRepositoryV3(dataSource);
        PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        memberService = new MemberServiceV3_1(transactionManager, memberRepository);
    }

    @AfterEach
    void after() throws SQLException {
        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);
    }

    @Test
    @DisplayName("정상 이체")
    void accountTransfer() throws SQLException {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);

        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);

        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(8000);
        assertThat(findMemberB.getMoney()).isEqualTo(12000);
    }

    @Test
    @DisplayName("이체중 예외 발생")
    void accountTransferEx() throws SQLException {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEx = new Member(MEMBER_EX, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);

        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberEx.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(10000);
        assertThat(findMemberB.getMoney()).isEqualTo(10000);
    }
}
```
- new DataSourceTransactionManager(dataSource) : JDBC 기술을 사용하므로 JDBC용 트랜잭션 매니저 사용

1. 서비스 계층에서 **transactionManager.getTransaction()** 을 호출해서 트랜잭션을 시작한다.
2. 트랜잭션을 시작하려면 먼저 데이터베이스 커넥션이 필요하다. 트랜잭션 매니저는 내부에서 **데이터소스를 사용해서 커넥션을 생성**한다.
3. 커넥션을 **수동 커밋 모드로 변경해서 실제 데이터베이스 트랜잭션을 시작**한다.
4. 커넥션을 트랜잭션 동기화 매니저에 보관한다.
5. 트랜잭션 동기화 매니저는 쓰레드 로컬에 커넥션을 보관한다. 따라서 멀티 쓰레드 환경에 안전하게 커넥션을 보관할 수 있다.
6. 서비스는 비즈니스 로직을 실행하면서 리포지토리의 메서드들을 호출한다. 이때 **커넥션을 파라미터로 전달하지 않는다**.
7. 리포지토리 메서드들은 트랜잭션이 시작된 커넥션이 필요하다. 리포지토리는 **DataSourceUtils.getConnection() 을 사용해서 트랜잭션 동기화 매니저에 보관된 커넥션을 꺼내서 사용**한다. 이 과정을 통해서 자연스럽게 같은 커넥션을 사용하고, 트랜잭션도 유지된다.
8. 획득한 커넥션을 사용해서 SQL을 데이터베이스에 전달해서 실행한다.
9. 비즈니스 로직이 끝나고 트랜잭션을 종료한다. 트랜잭션은 커밋하거나 롤백하면 종료된다.
10. 트랜잭션을 종료하려면 동기화된 커넥션이 필요하다. 트랜잭션 동기화 매니저를 통해 동기화된 커넥션을 획득한다.
11. 획득한 커넥션을 통해 데이터베이스에 트랜잭션을 커밋하거나 롤백한다.
12. 전체 리소스를 정리한다.
    - 트랜잭션 동기화 매니저를 정리한다. 쓰레드 로컬은 사용후 꼭 정리해야 한다.
    - con.setAutoCommit(true) 로 되돌린다. 커넥션 풀을 고려해야 한다.
    - con.close() 를 호출해서 커넥션을 종료한다. 커넥션 풀을 사용하는 경우 con.close() 를 호출하면 커넥션 풀에 반환된다.

#### 트랜잭션 추상화 결과
- 트랜잭션 추상화 덕분에 서비스 코드는 이제 JDBC 기술에 의존하지 않는다. 이후 JDBC에서 JPA로 변경해도 서비스 코드를 그대로 유지할 수 있다.
- 기술 변경시 의존관계 주입만 DataSourceTransactionManager 에서 JpaTransactionManager 로 변경해주면 된다.
- java.sql.SQLException 이 아직 남아있지만 이 부분은 뒤에 예외 문제에서 해결하자. 트랜잭션 동기화 매니저 덕분에 커넥션을 파라미터로 넘기지 않아도 된다.


### 트랜잭션 템플릿
- 트랜잭션 사용 코드에서 자주 반복되는 부분(try, catch, finally..)을 트랜잭션 템플릿을 활용해 깔끔하게 정리할 수 있다.

```java
	public class TransactionTemplate {
		private PlatformTransactionManager transactionManager;
		public <T> T execute(TransactionCallback<T> action){..}
		void executeWithoutResult(Consumer<TransactionStatus> action){..}
	}
```
- execute() : 응답 값이 있을 때 사용한다.
- executeWithoutResult() : 응답 값이 없을 때 사용한다.

#### 트랜잭션 템플릿 적용 예제
```java
@Slf4j
public class MemberServiceV3_2 {
    private final TransactionTemplate txTemplate;
    private final MemberRepositoryV3 memberRepository;

    public MemberServiceV3_2(PlatformTransactionManager transactionManager, MemberRepositoryV3 memberRepository) {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.memberRepository = memberRepository;
    }

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {

        txTemplate.executeWithoutResult((status) -> {
            //비즈니스 로직
            try {
                bizLogic(fromId, toId, money);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void bizLogic(String fromId, String toId, int money) throws SQLException {
        ...
    }

    private static void validation(Member toMember) {
        ...
    }
}
```
- 트랜잭션 템플릿 덕분에 트랜잭션을 시작하고, 커밋하거나 롤백하는 코드가 모두 제거되었다.
- 기본 동작 흐름

    - 비즈니스 로직이 정상 수행되면 커밋
    - 언체크 예외가 발생하면 롤백

> 트랜잭션 템플릿 덕분에 반복되는 코드를 제거할 수 있었다. 하지만 이곳은 서비스 계층인데 비즈니스 로직 뿐만 아니라 트랜잭션 처리 로직도 포함되어 있다. 이를 분리하고자 한다.

## 트랜잭션 AOP
- 트랜잭션 템플릿 덕분에 트랜잭션을 처리하는 반복 코드는 해결할 수 있었다. 하지만 서비스 계층에 순수한 비즈니스 로직만 남기지는 못했다.  이럴 때 스프링 AOP를 통해 프록시를 도입하면 문제를 깔끔하게 해결할 수 있다.

#### AOP 프록시 도입 전
![](https://velog.velcdn.com/images/bon0057/post/1fa1b2ff-b479-4e30-a3f1-1884363b8327/image.png)

#### AOP 프록시 도입 후
![](https://velog.velcdn.com/images/bon0057/post/208fa93f-d276-44be-882c-586de127c056/image.png)

- 스프링이 제공하는 AOP 기능을 사용하면 프록시를 매우 편리하게 적용할 수 있다.
- 스프링 부트를 사용하면 트랜잭션 AOP를 처리하기 위해 필요한 스프링 빈들도 자동으로 등록해준다.
- 개발자는 트랜잭션 처리가 필요한 곳에 **@Transactional 애노테이션**만 붙여주면 된다. 스프링의 트랜잭션 AOP는 이 애노테이션을 인식해서 트랜잭션 프록시를 적용해준다.


#### AOP 프록시 도입한 서비스 계층
```java
@Slf4j
public class MemberServiceV3_3 {

    private final MemberRepositoryV3 memberRepository;

    public MemberServiceV3_3(MemberRepositoryV3 memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        bizLogic(fromId, toId, money);
    }

    private void bizLogic(String fromId, String toId, int money) throws SQLException {
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private static void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
```
- 순수한 비즈니스 로직만 남기고 트랜잭션 관련 코드는 모두 제거했다.
- 스프링이 제공하는 트랜잭션 AOP를 적용하기 위해 @Transactional 어노테이션을 추가했다.
- @Transactional 어노테이션은 메서드에 붙여도 되고, 클래스에 붙여도 된다.

#### AOP 프록시 도입 테스트
```java
@Slf4j
@SpringBootTest
class MemberServiceV3_3Test {

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    @Autowired
    private MemberRepositoryV3 memberRepository;
    @Autowired
    private MemberServiceV3_3 memberService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new DataSourceTransactionManager(dataSource());
        }

        @Bean
        MemberRepositoryV3 memberRepositoryV3() {
            return new MemberRepositoryV3(dataSource());
        }

        @Bean
        MemberServiceV3_3 memberServiceV3_3() {
            return new MemberServiceV3_3(memberRepositoryV3());
        }
    }


    @AfterEach
    void after() throws SQLException {
        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);
    }

    @Test
    @DisplayName("정상 이체")
    void accountTransfer() throws SQLException {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);

        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);

        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(8000);
        assertThat(findMemberB.getMoney()).isEqualTo(12000);
    }

    @Test
    @DisplayName("이체중 예외 발생")
    void accountTransferEx() throws SQLException {
        Member memberA = new Member(MEMBER_A, ![](https://velog.velcdn.com/images/bon0057/post/6827d13b-6bae-4a79-92ff-3706610df33d/image.png)
10000);
        Member memberEx = new Member(MEMBER_EX, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);

        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberEx.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(10000);
        assertThat(findMemberB.getMoney()).isEqualTo(10000);
    }
}
```
- @SpringBootTest : **스프링 AOP를 적용하려면 스프링 컨테이너가 필요**하다. 이 애노테이션이 있으면 테스트시 스프링 부트를 통해 스프링 컨테이너를 생성한다. 그리고 테스트에서 @Autowired 등을 통해 스프링 컨테이너가 관리하는 빈들을 사용할 수 있다.
- @TestConfiguration : 테스트 안에서 내부 설정 클래스를 만들어서 사용하면서 이 에노테이션을 붙이면, 스프링 부트가 자동으로 만들어주는 빈들에 추가로 필요한 스프링 빈들을 등록하고 테스트를 수행할 수 있다.
- TestConfig
    - DataSource 스프링에서 기본으로 사용할 데이터소스를 스프링 빈으로 등록한다. 추가로 트랜잭션 매니저에서도 사용한다.
    - DataSourceTransactionManager 트랜잭션 매니저를 스프링 빈으로 등록한다.
    - 스프링이 제공하는 트랜잭션 AOP는 스프링 빈에 등록된 트랜잭션 매니저를 찾아서 사용하기 때문에 트랜잭션 매니저를 스프링 빈으로 등록해두어야 한다.

### 트랜잭션 AOP 흐름
![](https://velog.velcdn.com/images/bon0057/post/89d84ec8-a319-48e6-9ec1-bfe78b66f39d/image.png)

- 스프링이 제공하는 선언적 트랜잭션 관리 덕분에 드디어 트랜잭션 관련 코드를 순수한 비즈니스 로직에서 제거할 수 있었다.
- 개발자는 트랜잭션이 필요한 곳에 @Transactional 애노테이션 하나만 추가하면 된다. 나머지는 스프링 트랜잭션 AOP가 자동으로 처리해준다.


### 스프링 부트의 자동 리소스 등록
```java
	@Bean
	DataSource dataSource() {
		return new DriverManagerDataSource(URL, USERNAME, PASSWORD);
	}

	@Bean
	PlatformTransactionManager transactionManager() {
		return new DataSourceTransactionManager(dataSource());
	}
```
- 기존에는 데이터소스와 트랜잭션 매니저를 직접 스프링 빈으로 등록했다.

#### 데이터 소스 자동 등록
- 스프링 부트는 데이터소스를 스프링 빈에 자동으로 등록한다.
- 자동으로 등록되는 스프링 빈 이름: dataSource
- 개발자가 직접 데이터소스를 빈으로 등록하면 스프링 부트는 데이터소스를 자동으로 등록하지 않는다.
- 스프링 부트는 다음과 같이 application.properties 에 있는 속성을 사용해서 DataSource 를 생성한다. 그리고 스프링 빈에 등록한다.

    - spring.datasource.url=jdbc:h2:tcp://localhost/~/test
        - spring.datasource.username=sa
        - spring.datasource.password=
- 스프링 부트가 기본으로 생성하는 데이터소스는 커넥션풀을 제공하는 HikariDataSource 이다. 커넥션풀과 관련된 설정도 application.properties 를 통해서 지정할 수 있다.
- spring.datasource.url 속성이 없으면 내장 데이터베이스(메모리 DB)를 생성하려고 시도한다.

#### 트랜잭션 매니저 자동 등록
- 스프링 부트는 적절한 트랜잭션 매니저( PlatformTransactionManager )를 자동으로 스프링 빈에 등록한다.
- 자동으로 등록되는 스프링 빈 이름: transactionManager
- 개발자가 직접 트랜잭션 매니저를 빈으로 등록하면 스프링 부트는 트랜잭션 매니저를 자동으로 등록하지 않는다.
- 어떤 트랜잭션 매니저를 선택할지는 현재 등록된 라이브러리를 보고 판단하는데, JDBC를 기술을 사용하면 DataSourceTransactionManager 를 빈으로 등록하고, JPA를 사용하면 JpaTransactionManager 를 빈으로 등록한다. 둘다 사용하는 경우 JpaTransactionManager 를 등록한다.

#### 테스트
```java
package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV3;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest
class MemberServiceV3_3Test {

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    @Autowired
    private MemberRepositoryV3 memberRepository;
    @Autowired
    private MemberServiceV3_3 memberService;

    /*@TestConfiguration
    static class TestConfig {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new DataSourceTransactionManager(dataSource());
        }

        @Bean
        MemberRepositoryV3 memberRepositoryV3() {
            return new MemberRepositoryV3(dataSource());
        }

        @Bean
        MemberServiceV3_3 memberServiceV3_3() {
            return new MemberServiceV3_3(memberRepositoryV3());
        }
    }*/


    @AfterEach
    void after() throws SQLException {
        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);
    }

    @Test
    @DisplayName("정상 이체")
    void accountTransfer() throws SQLException {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);

        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);

        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(8000);
        assertThat(findMemberB.getMoney()).isEqualTo(12000);
    }

    @Test
    @DisplayName("이체중 예외 발생")
    void accountTransferEx() throws SQLException {
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEx = new Member(MEMBER_EX, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);

        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberEx.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(10000);
        assertThat(findMemberB.getMoney()).isEqualTo(10000);
    }
}
```
- 데이터 소스와 트랜잭션 매니저를 수동 등록하지 않았고 서비스 계층, 데이터 접근 계층에도 각각 @Service, @Repository 애노테이션을 붙여 스프링 빈으로 자동 등록했다.

> 데이터소스와 트랜잭션 매니저는 스프링 부트가 제공하는 자동 빈 등록 기능을 사용하는 것이 편리하다. 추가로 application.properties 를 통해 설정도 편리하게 할 수 있다.


## 예외 계층
![](https://velog.velcdn.com/images/bon0057/post/4909e63b-2ef3-4453-9d61-df3202d70e6a/image.png)

### 체크 예외
- 기본적으로 언체크(런타임) 예외를 사용하자.
- 체크 예외는 비즈니스 로직상 의도적으로 던지는 예외에만 사용하자.
- 체크 예외 예시

    - 계좌 이체 실패 예외
    - 결제시 포인트 부족 예외 로그인 ID, PW 불일치 예외
- 계좌 이체 실패처럼 매우 심각한 문제는 개발자가 실수로 예외를 놓치면 안된다고 판단할 수 있다. 이 경우 체크 예외로 만들어 두면 컴파일러를 통해 놓친 예외를 인지할 수 있다.

#### 체크 예외 문제점
![](https://velog.velcdn.com/images/bon0057/post/cc3589b2-d5a9-4ddd-ad8b-a5d65bacdd55/image.png)

- 서비스 계층은  두 곳에서 올라오는 체크 예외인 SQLException 과 ConnectException 을 처리해야 한다. 그런데 서비스는 이 둘을 처리할 방법을 모른다. ConnectException 처럼 연결이 실패하거나, SQLException 처럼 데이터베이스에서 발생하는 문제처럼 심각한 문제들은 대부분 애플리케이션 로직에서 처리할 방법이 없다.
- 서비스 계층은 SQLException 과 ConnectException 를 처리할 수 없으므로 둘다 밖으로 던진다. 체크 예외이기 때문에 던질 경우 다음과 같이 선언해야 한다. method() throws SQLException, ConnectException
- 컨트롤러도 두 예외를 처리할 방법이 없다. 다음을 선언해서 예외를 밖으로 던진다. method() throws SQLException, ConnectException

### 체크 예외와 의존
- 앞서 계속 설명하고 있듯이 서비스 계층은 특정 구현 기술에 의존하지 않고 순수하게 유지해야 한다. 그러기 위해선 **예외에 대한 의존**도 함께 해결해야 한다.
- 서비스가 해결할 수 없는 SQLException 등에 관한 의존성을 없애야 한다는 것이다.

#### 체크 예외 구현 기술 변경시 파급 효과
![](https://velog.velcdn.com/images/bon0057/post/0b20e913-b951-4b27-8a0b-3db459abe68b/image.png)

### 언체크 예외
![](https://velog.velcdn.com/images/bon0057/post/1a8bc410-73c5-454d-b870-aede54b22823/image.png)
- 런타임 예외이기 때문에 서비스, 컨트롤러는 해당 예외들을 처리할 수 없다면 별도의 선언 없이 그냥 두면 된다

#### 언체크 예외 구현 기술 변경시 파급 효과
![](https://velog.velcdn.com/images/bon0057/post/9e0944a4-4dd1-4878-b645-ddfb4d477b3e/image.png)
- 런타임 예외를 사용하면 중간에 기술이 변경되어도 해당 예외를 사용하지 않는 컨트롤러, 서비스에서는 코드를 변경하지 않아도 된다.
- 구현 기술이 변경되는 경우, 예외를 공통으로 처리하는 곳에서는 예외에 따른 다른 처리가 필요할 수 있다. 하지만 공통 처리하는 한곳만 변경하면 되기 때문에 변경의 영향 범위는 최소화 된다.




### 체크 예외와 인터페이스
![](https://velog.velcdn.com/images/bon0057/post/5ef65c07-74b3-43c1-ae93-6ca3905dcc24/image.png)
```java
	public interface MemberRepository {
		Member save(Member member);
		Member findById(String memberId);
		void update(String memberId, int money);
		void delete(String memberId);
	}
```
- 인터페이스를 도입하고 서비스 계층이 데이터 구현 계층 **인터페이스에 의존**하게 됨으로써 특정 기술에 종속되지 않는다.

- 하지만 현재 SQLException이 체크 예외이므로 인터페이스에도 체크 예외가 선언되어 있어야 한다.

```java
	public interface MemberRepositoryEx {
		Member save(Member member) throws SQLException;
		Member findById(String memberId) throws SQLException;
		void update(String memberId, int money) throws SQLException;
		void delete(String memberId) throws SQLException;
}
```

### 체크 예외 종속
- 구현 기술을 쉽게 변경하기 위해서 인터페이스를 도입하더라도 SQLException과 같은 특정 구현 기술(JDBC)에 종속적인 체크 예외를 사용하게 되면 인터페이스에도 해당 예외를 포함해야 하므로 결국 종속적이게 된다.

### 런타임 예외
- 런타임 예외는 이런 부분에서 자유롭다. 인터페이스에 따로 런타임 예외를 선언하지 않아도 되기에 특정 기술에 종속적일 필요가 없다.

### 런타임 예외 적용
#### 런타임 예외를 상속하는 새로운 예외 정의
```java
	public class MyDbException extends RuntimeException{

    	public MyDbException() {
        	super();
    	}

    	public MyDbException(String message) {
        	super(message);
    	}

    	public MyDbException(String message, Throwable cause) {
        	super(message, cause);
    	}

    	public MyDbException(Throwable cause) {
        	super(cause);
    	}
	}
```

#### 런타임 예외 적용한 데이터 접근 계층
```java
/**
 * 예외 누수 문제 해결
 * 체크 예외를 런타임(언체크) 예외로 변경
 * MemberRepository 인터페이스 사용
 * throws SQLException 제거
 */
@Slf4j
public class MemberRepositoryV4_1 implements MemberRepository{

    private final DataSource dataSource;

    public MemberRepositoryV4_1(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Member save(Member member){
        String sql = "insert into member(member_id, money) values (?, ?)";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, member.getMemberId());
            pstmt.setInt(2, member.getMoney());
            pstmt.executeUpdate();
            return member;
        } catch (SQLException e) {
        	// 언체크 예외 던지기
            throw new MyDbException(e);
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public Member findById(String memberId){
        String sql = "select * from member where member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                Member member = new Member();
                member.setMemberId(rs.getString("member_id"));
                member.setMoney(rs.getInt("money"));
                return member;
            } else {
                throw new NoSuchElementException("member not found memberId=" + memberId);
            }

        } catch (SQLException e) {
        	// 언체크 예외 던지기
            throw new MyDbException(e);
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public void update(String memberId, int money){
        String sql = "update member set money=? where member_id=?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, money);
            pstmt.setString(2, memberId);
            int resultSize = pstmt.executeUpdate();
            log.info("resultSize = {}", resultSize);
        } catch (SQLException e) {
        	// 언체크 예외 던지기 
            throw new MyDbException(e);
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void delete(String memberId){
        String sql = "delete from member where member_id=?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
        	// 언체크 예외 던지기 
            throw new MyDbException(e);
        } finally {
            close(con, pstmt, null);
        }
    }

    private void close(Connection con, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        DataSourceUtils.releaseConnection(con, dataSource);
    }

    private Connection getConnection() throws SQLException {

        Connection con = DataSourceUtils.getConnection(dataSource);
        log.info("get Connection={}, class ={}", con, con.getClass());
        return con;
    }

}
```
- SQLException 대신에 새로 만든 언체크 예외인 MyDbException을 생성해서 던진다.

#### 런타임 예외 적용한 서비스 계층
```java
@Slf4j
public class MemberServiceV4 {
    private final MemberRepository memberRepository;

    public MemberServiceV4(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void accountTransfer(String fromId, String toId, int money){
        bizLogic(fromId, toId, money);
    }

    private void bizLogic(String fromId, String toId, int money){
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private static void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
```
- MemberRepository **인터페이스에 의존**
- 메서드에서 throws Exception 부분을 제거했다.

> 체크 예외를 런타임 예외로 변환하면서 인터페이스와 서비스 계층의 순수함을 유지할 수 있게 되었다. JDBC가 아닌 DB 접근 기술을 변경하더라도 서비스 계층의 코드를 변경하지 않고 유지할 수 있다.

## 데이터 접근 예외 제작
- 데이터베이스 오류에 따라서 특정 예외는 **복구**하고 싶을 수 있다.

![](https://velog.velcdn.com/images/bon0057/post/c6bd684e-d0a1-4e51-bb07-878c719a3d0b/image.png)
- SQLException 내부에 있는 **errorCode**를 활용하면 데이터베이스에서 어떤 문제가 발생했는지 구체적으로 알 수 있고 각 문제에 따라 달리 대응할 수 있다.

#### MyDuplicateKeyException
```java
public class MyDuplicateKeyException extends MyDbException {
    public MyDuplicateKeyException() {
    }

    public MyDuplicateKeyException(String message) {
        super(message);
    }

    public MyDuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyDuplicateKeyException(Throwable cause) {
        super(cause);
    }
}
```

#### 테스트
```java
public class ExTranslatorV1Test {

    Repository repository;
    Service service;

    @BeforeEach
    void init() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        repository = new Repository(dataSource);
        service = new Service(repository);
    }

    @Test
    void duplicateKeySave() {
        service.create("myId");
        service.create("myId");
    }

    @Slf4j
    @RequiredArgsConstructor
    static class Service {
        private final Repository repository;

        public void create(String memberId) {
            try {
                repository.save(new Member(memberId, 0));
                log.info("saveId={}", memberId);
            } catch (MyDuplicateKeyException e) {
                log.info("키 중복, 복구 시도");
                String retryId = generateNewId(memberId);
                log.info("retryId={}", retryId);
                repository.save(new Member(retryId, 0));
            } catch (MyDbException e) {
                log.info("데이터 접근 계층 예외", e);
                throw e;
            }
        }

        private String generateNewId(String memberId) {
            return memberId + new Random().nextInt(10000);
        }
    }

    @RequiredArgsConstructor
    static class Repository {

        private final DataSource dataSource;

        public Member save(Member member) {
            String sql = "insert into member(member_id, money) values(?,?)";
            Connection con = null;
            PreparedStatement pstmt = null;

            try {
                con = dataSource.getConnection();
                pstmt = con.prepareStatement(sql);
                pstmt.setString(1, member.getMemberId());
                pstmt.setInt(2, member.getMoney());
                pstmt.executeUpdate();
                return member;
            } catch (SQLException e) {
                if (e.getErrorCode() == 23505) {
                    throw new MyDuplicateKeyException(e);
                }
                throw new MyDbException(e);
            } finally {
                JdbcUtils.closeStatement(pstmt);
                JdbcUtils.closeConnection(con);
            }
        }
    }
}
```
#### Repository 핵심 코드
```java
	catch (SQLException e) {
		if (e.getErrorCode() == 23505) {
			throw new MyDuplicateKeyException(e);
		}
		throw new MyDbException(e);
	}
```
- 오류 코드가 키 중복 오류(23505)인 경우 MyDuplicateKeyException을 새로 만들어서 서비스 계층에 던진다. 나머지는 기존에 만들었던 MyDbException을 던진다.

#### Service 핵심 코드
```java
	try {
		repository.save(new Member(memberId, 0));
		log.info("saveId={}", memberId);
	} catch (MyDuplicateKeyException e) {
		log.info("키 중복, 복구 시도");
		String retryId = generateNewId(memberId);
		log.info("retryId={}", retryId);
		repository.save(new Member(retryId, 0));
	} catch (MyDbException e) {
		log.info("데이터 접근 계층 예외", e);
		throw e;
	}
```
- 처음에 저장을 시도하고 레퍼지토리에서 MyDuplciateKeyException 예외가 올라오면 이 예외를 잡고 새로운 ID 생성을 시도하고 다시 저장한다. <- 예외 복구
- 현재 복구할 수 없는 예외라면 다시 예외를 던진다. <- 예외 공통 처리하는 부분까지 전달

> SQL ErrorCode로 데이터베이스에 어떤 오류가 있는지 구체적으로 알 수 있고, 예외 변환을 통해 SQLException을 특정 기술에 종속적이지 않은 언체크 예외들로 변환할 수 있고 레퍼지토리에서 이 예외 변환을 해줌으로써 서비스 계층에서는 특정 기술에 종속적이지 않는 순수함을 유지할 수 있다.

## 스프링 예외 추상화
- 스프링은 데이터 접근과 관련된 예외를 추상화해서 제공한다. '
  ![](https://velog.velcdn.com/images/bon0057/post/2f07b09c-ed87-41fa-bd0d-89572500bf0e/image.png)
- 각각의 예외는 특정 기술에 종속적이지 않게 설계되어 있다. 서비스 계층에서도 스프링이 제공하는 예외를 사용할 수 있다.
- 예외의 최고 상위는 **DataAccessException**이다. 런타임 예외를 상속 받았기 때문에 스프링이 제공하는 데이터 접근 계층의 모든 예외는 런타임 예외이다.
    - Transient 예외 : 동일한 SQL을 다시 시도했을 때 성공할 가능성이 있다.
    - NonTransient 예외 : 동일한 SQL을 다시 시도하면 실패한다. 문법 오류, 제약 조건 위배 등

### 스프링 예외 변환기
```java
	SQLExceptionTranslator exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
  	DataAccessException resultEx = exTranslator.translate("select", sql, e);
```
- translate 메서드의 첫번째 파라미터는 읽을 수 있는 설명이고, 두번째는 실행한 sql, 마지막은 발생된 SQLException을 전달한다. 적절한 스프링 데이터 접근 계층의 예외로 변환해서 반환해준다.

### 스프링 예외 추상화 정리
- 스프링은 데이터 접근 계층에 대한 일관된 예외 추상화를 제공한다.
- 스프링은 예외 변환기를 통해서 SQLException 의 ErrorCode 에 맞는 적절한 스프링 데이터 접근 예외로 변환해준다.
- 만약 서비스, 컨트롤러 계층에서 예외 처리가 필요하면 특정 기술에 종속적인 SQLException 같은 예외를 직접 사용하는 것이 아니라, 스프링이 제공하는 데이터 접근 예외를 사용하면 된다.
- 스프링 예외 추상화 덕분에 특정 기술에 종속적이지 않게 되었다. 이제 JDBC에서 JPA같은 기술로 변경되어도 예외로 인한 변경을 최소화 할 수 있다. 향후 JDBC에서 JPA로 구현 기술을 변경하더라도, 스프링은 JPA 예외를 적절한 스프링 데이터 접근 예외로 변환해준다.
- 물론 스프링이 제공하는 예외를 사용하기 때문에 스프링에 대한 기술 종속성은 발생한다. 스프링에 대한 기술 종속성까지 완전히 제거하려면 예외를 모두 직접 정의하고 예외 변환도 직접 하면 되지만, 실용적인 방법은 아니다.

### 스프링 예외 추상화 적용
```java
/**
 * SQLExceptionTranslator
 */
@Slf4j
public class MemberRepositoryV4_2 implements MemberRepository{

    private final DataSource dataSource;
    private final SQLExceptionTranslator exTranslator;

    public MemberRepositoryV4_2(DataSource dataSource) {
        this.dataSource = dataSource;
        this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    }

    @Override
    public Member save(Member member){
        String sql = "insert into member(member_id, money) values (?, ?)";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, member.getMemberId());
            pstmt.setInt(2, member.getMoney());
            pstmt.executeUpdate();
            return member;
        } catch (SQLException e) {
            throw exTranslator.translate("save", sql, e);
            //throw new MyDbException(e);
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public Member findById(String memberId){
        String sql = "select * from member where member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                Member member = new Member();
                member.setMemberId(rs.getString("member_id"));
                member.setMoney(rs.getInt("money"));
                return member;
            } else {
                throw new NoSuchElementException("member not found memberId=" + memberId);
            }

        } catch (SQLException e) {
            throw exTranslator.translate("findById", sql, e);
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public void update(String memberId, int money){
        String sql = "update member set money=? where member_id=?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, money);
            pstmt.setString(2, memberId);
            int resultSize = pstmt.executeUpdate();
            log.info("resultSize = {}", resultSize);
        } catch (SQLException e) {
            throw exTranslator.translate("update", sql, e);
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public void delete(String memberId){
        String sql = "delete from member where member_id=?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw exTranslator.translate("delete", sql, e);
        } finally {
            close(con, pstmt, null);
        }
    }

    private void close(Connection con, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        DataSourceUtils.releaseConnection(con, dataSource);
        //JdbcUtils.closeConnection(con);
    }

    private Connection getConnection() throws SQLException {

        Connection con = DataSourceUtils.getConnection(dataSource);
        //Connection con = dataSource.getConnection();
        log.info("get Connection={}, class ={}", con, con.getClass());
        return con;
    }

}
```

## JDBC 반복 문제 해결
- 데이터 접근 계층에서 JDBC 반복 문제

    - 커넥션 조회, 커넥션 동기화
    - PreparedStatement 생성 및 파라미터 바인딩 쿼리 실행
    - 결과 바인딩
    - 예외 발생시 스프링 예외 변환기 실행
    - 리소스 종료

### JDBC Template - 반복 제거
```java
/**
 * JDBC Template 적용 - 반복 문제 해결
 */
@Slf4j
public class MemberRepositoryV5 implements MemberRepository{

    private final JdbcTemplate template;

    public MemberRepositoryV5(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    @Override
    public Member save(Member member){
        String sql = "insert into member(member_id, money) values (?, ?)";
        template.update(sql, member.getMemberId(), member.getMoney());
        return member;

        /*Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, member.getMemberId());
            pstmt.setInt(2, member.getMoney());
            pstmt.executeUpdate();
            return member;
        } catch (SQLException e) {
            throw exTranslator.translate("save", sql, e);
            //throw new MyDbException(e);
        } finally {
            close(con, pstmt, null);
        }*/
    }

    @Override
    public Member findById(String memberId){
        String sql = "select * from member where member_id = ?";
        return template.queryForObject(sql, memberRowMapper(), memberId);
    }

    private RowMapper<Member> memberRowMapper() {
        return (rs, rowNum) -> {
            Member member = new Member();
            member.setMemberId(rs.getString("member_id"));
            member.setMoney(rs.getInt("money"));
            return member;
        };
    }

    @Override
    public void update(String memberId, int money){
        String sql = "update member set money=? where member_id=?";
        template.update(sql, money, memberId);
    }

    @Override
    public void delete(String memberId){
        String sql = "delete from member where member_id=?";
        template.update(sql, memberId);
    }
}

```
- JdbcTemplate은 JDBC로 개발할 때 발생하는 반복을 대부분 해결해준다. 트랜잭션을 위한 커넥션 동기화, 예외 발생 시 스프링 예외 변환기도 자동으로 실행해준다. 