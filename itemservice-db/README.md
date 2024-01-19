## 데이터 접근 기술
### SQLMapper
- 개발자는 SQL만 작성하면 해당 SQL의 결과를 객체로 편리하게 매핑해준다.
- JDBC를 직접 사용할 때 발생하는 여러가지 중복을 제거해주고 여러 편리한 기능을 제공해준다.
#### JdbcTemplate
#### MyBatis


### ORM 기반
- JdbcTemplate이나 MyBatis같은 SQL 매퍼 기술은 SQL을 개발자가 직접 작성해야 하지만 JPA를 사용하면 기본적인 SQL은 JPA가 대신 작성하고 처리해준다. 개발자는 저장하고 싶으 객체를 마치 자바 컬렉션에 저장하고 조회하듯 ORM 기술이 데이터베이스에 해당 객체를 저장하고 조회해준다.
- JPA는 자바 진영의 ORM 표준이고, Hibernate는 JPA에서 가장 많이 사용하는 구현체이다.
- 스프링 데이터 JPA, Querydsl은 JPA를 더 편리하게 사용할 수 있도록 도와주는 프로젝트이다.
#### JPA, Hibernate
#### 스프링 데이터 JPA
#### Querydsl



> DTO(Data Transfer Object)
- 데이터 전송 객체이며 기능은 없고 데이터를 전달만 하는 용도로 사용되는 객체를 뜻한다.

## 예제
### 상품 도메인
```java

@Data
public class Item {
    private Long id;

    private String itemName;
    private Integer price;
    private Integer quantity;

    public Item() {
    }

    public Item(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }
}
```
### 데이터 접근 계층 인터페이스
```java
public interface ItemRepository {

    Item save(Item item);

    void update(Long itemId, ItemUpdateDto updateParam);

    Optional<Item> findById(Long id);

    List<Item> findAll(ItemSearchCond cond);

}
```

### 데이터 접근 계층 구현체 - 메모리
```java
@Repository
public class MemoryItemRepository implements ItemRepository {

    private static final Map<Long, Item> store = new HashMap<>(); //static
    private static long sequence = 0L; //static

    @Override
    public Item save(Item item) {
        item.setId(++sequence);
        store.put(item.getId(), item);
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = findById(itemId).orElseThrow();
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
    }

    @Override
    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();
        return store.values().stream()
                .filter(item -> {
                    if (ObjectUtils.isEmpty(itemName)) {
                        return true;
                    }
                    return item.getItemName().contains(itemName);
                }).filter(item -> {
                    if (maxPrice == null) {
                        return true;
                    }
                    return item.getPrice() <= maxPrice;
                })
                .collect(Collectors.toList());
    }

    public void clearStore() {
        store.clear();
    }

}
```

### 서비스 계층 구현체
```java
package hello.itemservice.service;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ItemServiceV1 implements ItemService {

    private final ItemRepository itemRepository;

    @Override
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        itemRepository.update(itemId, updateParam);
    }

    @Override
    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    @Override
    public List<Item> findItems(ItemSearchCond cond) {
        return itemRepository.findAll(cond);
    }
}
```

### 컨트롤러
#### HomeController
```java
@Controller
@RequiredArgsConstructor
public class HomeController {

    @RequestMapping("/")
    public String home() {
        return "redirect:/items";
    }
}
```

#### ItemController
```java
@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public String items(@ModelAttribute("itemSearch") ItemSearchCond itemSearch, Model model) {
        List<Item> items = itemService.findItems(itemSearch);
        model.addAttribute("items", items);
        return "items";
    }

    @GetMapping("/{itemId}")
    public String item(@PathVariable long itemId, Model model) {
        Item item = itemService.findById(itemId).get();
        model.addAttribute("item", item);
        return "item";
    }

    @GetMapping("/add")
    public String addForm() {
        return "addForm";
    }

    @PostMapping("/add")
    public String addItem(@ModelAttribute Item item, RedirectAttributes redirectAttributes) {
        Item savedItem = itemService.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/items/{itemId}";
    }

    @GetMapping("/{itemId}/edit")
    public String editForm(@PathVariable Long itemId, Model model) {
        Item item = itemService.findById(itemId).get();
        model.addAttribute("item", item);
        return "editForm";
    }

    @PostMapping("/{itemId}/edit")
    public String edit(@PathVariable Long itemId, @ModelAttribute ItemUpdateDto updateParam) {
        itemService.update(itemId, updateParam);
        return "redirect:/items/{itemId}";
    }

}
```

### 프로필
- 스프링은 로딩 시점에 **application.properties 의 spring.profiles.active** 속성을 읽어서 프로필로 사용한다. 이 프로필은 로컬, 운영 환경, 테스트 실행 등등 다양한 환경에 따라서 다른 설정을 할 때 사용하는 정보이다.

```java
spring.profiles.active = local
```
- /src/main 하위의 자바 객체를 실행할 때 동작하는 스프링 설정이다.

#### ItemServiceApplcication
```java
	@Import(MemoryConfig.class)
  	@SpringBootApplication(scanBasePackages = "hello.itemservice.web")
  	public class ItemServiceApplication {
    	public static void main(String[] args) {
        	SpringApplication.run(ItemServiceApplication.class, args);
		}
    
    	@Bean
    	@Profile("local")
    	public TestDataInit testDataInit(ItemRepository itemRepository) {
        	return new TestDataInit(itemRepository);
    	}
	}
```
- @Import를 통해 Configuration 파일을 설정할 수 있다.
- scanBasePackages를 통해 컴포넌트 스캔의 영역을 설정할 수 있다.
- @Profile을 통해 특정 프로필의 경우에만 해당 스프링 빈을 등록한다.

### 테스트
```java
@Transactional
@SpringBootTest
class ItemRepositoryTest {

    @Autowired
    ItemRepository itemRepository;

    @AfterEach
    void afterEach() {
        //MemoryItemRepository 의 경우 제한적으로 사용
        if (itemRepository instanceof MemoryItemRepository) {
            ((MemoryItemRepository) itemRepository).clearStore();
        }

    }

    @Test
    void save() {
        ...
        Item findItem = itemRepository.findById(item.getId()).get();
        assertThat(findItem).isEqualTo(savedItem);
    }

    @Test
    void updateItem() {
        ...
        ItemUpdateDto updateParam = new ItemUpdateDto("item2", 20000, 30);
        itemRepository.update(itemId, updateParam);

        Item findItem = itemRepository.findById(itemId).get();
        assertThat(findItem.getItemName()).isEqualTo(updateParam.getItemName());
        ...
    }

    @Test
    void findItems() {
       	...
        itemRepository.save(item1);
        itemRepository.save(item2);
        itemRepository.save(item3);

        //둘 다 없음 검증
        test(null, null, item1, item2, item3);
        test("", null, item1, item2, item3);
		...
    }

    void test(String itemName, Integer maxPrice, Item... items) {
        List<Item> result = itemRepository.findAll(new ItemSearchCond(itemName, maxPrice));
        assertThat(result).containsExactly(items);
    }
}
```
#### 인터페이스 테스트
- 인터페이스를 구현한 객체를 테스트하는 것이 아닌 인터페이스를 테스트하면 향후 다른 구현체로 변경되었을 때 해당 구현체가 잘 동작하는지 같은 테스트로 편리하게 검증할 수 있다.

#### DB 테이블 기본 키 선택 전략
##### 자연 키(Natural Key)
##### 대리 키(Surrogate Key)
- 대리 키 사용이 권장된다. 자연 키인 전화번호, 주민등록번호는 유저를 편하게 식별할 수 있는 키이지만 변경될 수 있는 요소가 있고 비즈니스 환경에 따라 사용하지 못하게 될 수 있다.

## 데이터 접근 기술 - JdbcTemplate
- JdbcTemplate은 spring-jdbc 라이브러리에 포함되어 있는데, 이 라이브러리는 스프링으로 JDBC를 사용할 때 기본으로 사용되는 라이브러리이다. 그리고 별도의 복잡한 설정 없이 바로 사용할 수 있다.
- JdbcTemplate은 템플릿 콜백 패턴을 사용해서, JDBC를 직접 사용할 때 발생하는 대부분의 반복 작업을 대신 처리해준다. 개발자는 SQL을 작성하고, 전달할 파리미터를 정의하고, 응답 값을 매핑하기만 하면 된다. 우리가 생각할 수 있는 대부분의 반복 작업을 대신 처리해준다.
    - 커넥션 획득
    - statement 를 준비하고 실행
    - 결과를 반복하도록 루프를 실행
    - 커넥션 종료, statement , resultset 종료 트랜잭션 다루기 위한 커넥션 동기화
    - 예외 발생시 스프링 예외 변환기 실행

### 데이터 접근 계층 구현체 - JdbcTemplate
```java
@Slf4j
public class JdbcTemplateRepositoryV1 implements ItemRepository {

    private final JdbcTemplate template;

    public JdbcTemplateRepositoryV1(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }
```
- JdbcTemplateRepostiroy는 ItemRepository 인터페이스를 구현했다.
- JdbcTemplate 자체를 주입받아도 되고 dataSource를 주입받아 내부에서 생성할 수 있다.
```java
    @Override
    public Item save(Item item) {
        String sql = "insert into item(item_name, price, quantity) values(?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(connection -> {
            //자동 증가 키
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, item.getItemName());
            ps.setInt(2, item.getPrice());
            ps.setInt(3, item.getQuantity());
            return ps;
        }, keyHolder);

        long key = keyHolder.getKey().longValue();
        item.setId(key);
        return item;
    }
```
- DB에 데이터를 저장하는 save 메서드이다. DB에 데이터를 변경할 때는 template.update()를 사용한다.
- 데이터를 저장할 때 Primary Key 생성을 개발자가 직접 하지 않고 **identity**(auto increment) 방식을 택했다.
- **KeyHolder**와 **connection.prepareStatement(sql, new String[] {"id"})** 를 사용해서 Insert Query 실행 후에 데이터베이스에서 생성된 ID값을 바로 조회할 수 있다.
```java
    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item set item_name=?, price=?, quantity=? where id=?";
        template.update(sql,
                updateParam.getItemName(),
                updateParam.getPrice(),
                updateParam.getQuantity(),
                itemId);
    }
```
- 데이터를 업데이트하는 update 메서드이다.
- sql을 작성한 후에 **?** 에 바인딩할 파라미터를 순서대로 전달하여 사용한다.
- template.update() 메서드의 반환값은 영향을 받은 로우 수 이다.
```java
    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id =?";
        try {
            Item item = template.queryForObject(sql, itemRowMapper(), id);
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
```
- ID를 통해 데이터를 하나 조회하는 findById 메서드이다.
- template.queryForObject() 메서드는 결과 로우가 하나일 때 사용하고 객체를 반환받아야 하기 때문에 **RowMapper** 가 필요하다.
- 결과가 없거나 결과가 둘 이상일 경우 예외가 발생한다.

```java
    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        String sql = "select id, item_name, price, quantity from item";

        //동적 쿼리
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;
        List<Object> param = new ArrayList<>();

        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',?,'%')";
            param.add(itemName);
            andFlag = true;
        }
        if (maxPrice != null) {
            if (andFlag) {
                sql += " and";
            }
            sql += " price <= ?";
            param.add(maxPrice);
        }
        log.info("sql={}", sql);
        return template.query(sql, itemRowMapper(), param.toArray());
    }
```
- 검색 조건을 통해 데이터를 리스트로 조회하는 findAll 메서드이다.
- template.query()는 객체를 반환받고 결과가 하나 이상일 때 사용한다.
- **RowMapper** 를 통해 데이터베이스의 반환 결과인 **ResultSet**을 객체로 변환받는다.
- 조건에 따라 데이터를 조회해야 하기에 조건에 따라 SQL을 바꾸는 동적 쿼리를 사용한다. SQL 중심인 JdbcTemplate은 동적 쿼리에 취약하다.
- 동적 쿼리 부분을 개선한 데이터 접근 기술이 이후에 사용해 볼 **MyBatis**이다.

```java
    private RowMapper<Item> itemRowMapper() {
        return ((rs, rowNum) -> {
            Item item = new Item();
            item.setId(rs.getLong("id"));
            item.setItemName(rs.getString("item_name"));
            item.setPrice(rs.getInt("price"));
            item.setQuantity(rs.getInt("quantity"));
            return item;
        });
    }
```
- 객체를 반환받을 때 사용한 RowMapper 메서드이다.
```java
while(resultSet 이 끝날 때 까지) { 
	rowMapper(rs, rowNum)
}
```
- 위와 같은 루프를 JdbcTemplate이 실행시켜준다고 생각하고 resultSet과 rowNum을 사용해 객체로 변환하기 위한 코드를 작성한다.

#### Configuration 설정 파일
```java
@Configuration
@RequiredArgsConstructor
public class JdbcTemplateV1Config {

    private final DataSource dataSource;

    @Bean
    public ItemService itemService() {
        return new ItemServiceV1(itemRepository());
    }

    @Bean
    public ItemRepository itemRepository() {
        return new JdbcTemplateRepositoryV1(dataSource);
    }
}
```
- ItemService 구현체와 ItemRepository 구현체를 수동으로 스프링 빈으로 등록하고 의존관계를 수동으로 주입했다.
- DataSource는 **스프링 부트가 스프링 빈으로 자동 등록** 하므로 주입받아서 사용하고 JdbcTemplateRepositoryV1에 dataSource를 주입해준다.

#### DataSource 자동 등록 - application.properties
```
spring.profiles.active=local
spring.datasource.url=jdbc:h2:tcp://localhost/~/test
spring.datasource.username=sa
```
- 이렇게 설정하면 스프링 부트가 **커넥션 풀**과 **DataSource**, **트랜잭션 매니저**를 스프링 빈으로 자동 등록한다.

### NamedParameterJdbcTemplate
```java
  String sql = "update item set item_name=?, quantity=?, price=? where id=?";
  template.update(sql,
          itemName,
          price,
          quantity,
          itemId);
```
- 개발자의 실수, 커뮤니케이션 미스로 발생할 수 있는 파라미터 바인딩 문제이다. 이런 문제를 해결하기 위해 JdbcTemplate은 NamedParameterJdbcTemplate을 제공한다.

- 기존의 update() 메서드에서는 sql의 **?** 부분에 바인딩할 파라미터들을 순서대로 넣어주었는데 NamedParameterJdbcTemplate에서는 **:파라미터이름**에 파라미터를 바인딩한다.
- 파라미터를 전달하려면 Map(Key, Value) 데이터 구조를 만들어 전달해야 한다.

#### 파라미터 종류
1. Map
2. SqlParameterSource (Interface)
    - MapSqlParameterDataSource
    - BeanPropertySqlParameterDataSource

### 데이터 접근 계층 구현체 - NamedParameterJdbcTemplate

```java
@Slf4j
public class JdbcTemplateRepositoryV2 implements ItemRepository {

    //private final JdbcTemplate template;
    private final NamedParameterJdbcTemplate template;

    public JdbcTemplateRepositoryV2(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
```
- JdbcTemplate -> NamedParameterJdbcTemplate

```java
    @Override
    public Item save(Item item) {
        String sql = "insert into item(item_name, price, quantity) " +
                "values (:itemName, :price, :quantity);";

        SqlParameterSource param = new BeanPropertySqlParameterSource(item);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        template.update(sql, param, keyHolder);

        long key = keyHolder.getKey().longValue();
        item.setId(key);
        return item;
    }
```
- BeanPropertySqlParamterSource를 통해 파라미터를 생성했다.
- **자바빈 프로퍼티 규약(getXXX(), setXXX())** 을 통해 파라미터를 자동으로 생성해주는 것이다.

```java
    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item set item_name=:itemName, price=:price, quantity=:quantity where id=:id";

        SqlParameterSource param = new MapSqlParameterSource()
                .addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId);
        template.update(sql, param);
    }
```
- Map과 유사한 **MapSqlParamterSource**를 사용하여 파라미터를 생성했다.

```java
    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id =:id";
        try {
            Map<String, Object> param = Map.of("id", id);
            Item item = template.queryForObject(sql, param, itemRowMapper());
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
```
- 간단하게 Map 데이터 구조를 사용하여 파라미터를 생성했다.

```java
    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        SqlParameterSource param = new BeanPropertySqlParameterSource(cond);

        String sql = "select id, item_name, price, quantity from item";

        //동적 쿼리
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;

        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',:itemName,'%')";
            andFlag = true;
        }
        if (maxPrice != null) {
            if (andFlag) {
                sql += " and";
            }
            sql += " price <= :maxPrice";
        }
        log.info("sql={}", sql);
        return template.query(sql, param, itemRowMapper());
    }

    private RowMapper<Item> itemRowMapper() {
        return BeanPropertyRowMapper.newInstance(Item.class);
    }
}
```
- 기존에 수동으로 객체로 변환하는 코드를 작성했던 RowMapper를 **BeanPropertyRowMapper**로 변경해서 사용했다. ResultSet의 결과를 받아서 자바빈 프로퍼티 규약에 맞춰 데이터를 변환, 객체를 생성해준다.
- 자바 객체는 camel 표기법을 사용하고 (itemName) 관계형 데이터베이스에서는 주로 언더스코어 표기법을 사용한다. (item_name)
- BeanPropertyRowMapper는 언더스코어 표기법을 카멜로 자동 변환해준다.

### SimpleJdbcInsert
- JdbcTemplate은 INSERT SQL을 직접 작성하지 않아도 되도록 **SimpleJdbcInsert** 기능을 제공한다.

```java
	private final SimpleJdbcInsert jdbcInsert;

    public JdbcTemplateRepositoryV3(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("item")
                .usingGeneratedKeyColumns("id");
    }
```
- withTableName() : 데이터를 저장할 DB 테이블 명을 지정
- usingGeneratedKeyColumns() : key를 생성하는 PK column 명을 지정

```java
    @Override
    public Item save(Item item) {
        SqlParameterSource param = new BeanPropertySqlParameterSource(item);
        Number key = jdbcInsert.executeAndReturnKey(param);
        item.setId(key.longValue());
        return item;
    }
```

### 스프링 JdbcTemplate 공식 매뉴얼
[JdbcTemplate 매뉴얼](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#jdbc-JdbcTemplate)





## 테스트 데이터베이스
### 데이터베이스 분리
application.properties 파일은 src/main/resources 와 src/test/resources에 각각 존재하는데 테스트 케이스는 src/test에 있기 때문에 실행하면 src/test에 있는 application.properties 파일이 우선순위를 가지고 실행된다.

```java
spring.profiles.active=test
spring.datasource.url=jdbc:h2:tcp://localhost/~/testcase
spring.datasource.username=sa
```
- jdbc:h2:tcp://localhost/~/test local에서 접근하는 서버 전용 데이터베이스
- jdbc:h2:tcp://localhost/~/testcase 테스트 케이스에서 사용하는 전용 데이터베이스
- 프로필과 DB를 main과 다르게 함으로써 독립적인 테스트를 진행할 수 있다.

```java
@SpringBootTest
class ItemRepositoryTest {
	...
}
```
- @SpringBootTest 어노테이션을 붙이면 테스트는 @SpringBootApplication을 찾아서 설정으로 사용한다.

### 데이터베이스 롤백
- 테스트의 중요한 원칙으로 **테스트는 다른 테스트와 격리**해야 하고 **테스트는 반복해서 실행**가능해야 한다.
- 테스트가 끝나고 나서 트랜잭션을 강제로 롤백해버리면 데이터가 깔끔하게 제거된다. 테스트를 하면서 데이터를 저장하고 중간에 예외가 발생해 종료되버려도 트랜잭션을 커밋하지 않았기에 데이터가 반영되지 않는다.

#### 직접 트랜잭션 추가
```java
	@SpringBootTest
  	class ItemRepositoryTest {
      
		@Autowired
      	ItemRepository itemRepository;

		//트랜잭션 관련 코드
		@Autowired
		PlatformTransactionManager transactionManager;

 		TransactionStatus status;
      
      	@BeforeEach
      	void beforeEach() {
			//트랜잭션 시작
          	status = transactionManager.getTransaction(new DefaultTransactionDefinition());
		}
      
      	@AfterEach
      	void afterEach() {
			//트랜잭션 롤백
          	transactionManager.rollback(status);
      	}
		...
	}
```
- 스프링 부트는 자동으로 적절한 트랜잭션 매니저를 스프링 빈으로 등록해주기에 주입받아서 사용할 수 있다.
- @BeforeEach 어노테이션이 부은 메서드는 각각의 테스트 케이스를 실행하기 직전에 호출된다. 여기서 트랜잭션을 시작하면 된다.
- @AfterEach 어노테이션이 붙은 메서드는 각각의 테스트 케이스가 완료된 직후에 호출되기에 여기서 트랜잭션을 롤백하면 데이터를 트랜잭션 실행 전 상태로 복구할 수 있다.

#### @Transactional
- 스프링은 테스트 데이터 초기화를 위해 트랜잭션을 적용하고 롤백하는 방식을 @Transactional 어노테이션 하나로 해결해준다.

```java
	@Transactional
  	@SpringBootTest
  	class ItemRepositoryTest {
    	...
    }
```
- @Transactional 어노테이션은 로직이 성공적으로 수행되면 커밋하도록 동작한다. 하지만 테스트에서 사용하면 특별하게 동작한다.
- @Transactional 이 테스트에 있으면 스프링은 테스트를 트랜잭션 안에서 실행하고 테스트가 끝나면 트랜잭션을 자동으로 롤백시켜 버린다.
- 트랜잭션을 테스트에서 시작하기에 중간에 서비스, 리포지토리에 있는 @Transactional도 테스트에서 시작한 트랜잭션에 참여한다.

```java
	@Commit
    @Transactional
    @SpringBootTest
    class ItemRepositoryTest {
    	...
    }
```
- 테스트 결과를 끝나고 확인하고 싶다면 @Commit 어노테이션을 붙이면 된다.

### 임베디드 모드 DB
- 테스트 케이스를 실행하기 위해서 별도의 데이터베이스를 설치하고 운영하는 것은 번잡하다. 테스트가 끝나면 데이터베이스의 데이터를 모두 삭제해도 되고 데이터베이스 자체도 삭제해도 된다.

#### 직접 사용
```java
@Import(V2Config.class)
@Slf4j
@SpringBootApplication(scanBasePackages = "hello.itemservice.web")
public class ItemServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItemServiceApplication.class, args);
	}

	@Bean
	@Profile("local")
	public TestDataInit testDataInit(ItemRepository itemRepository) {
		return new TestDataInit(itemRepository);
	}

	@Bean
	@Profile("test")
	public DataSource dataSource() {
		log.info("메모리 데이터베이스 초기화");
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		return dataSource;
	}
}
```
- 데이터소스를 만들 때 위와 같이 URL을 세팅하면 임베디드 모드(메모리 모드)로 동작하는 H2 데이터베이스를 사용할 수 있다.
- 이대로 실행하면 오류가 발생한다. 메모리 DB는 매번 만들어지는 것이기 때문에 테이블 정보가 안에 들어 있지 않다. 스프링 부트는 직접 테이블을 생성하는 번거로움을 없애준다.

**src/test/resources/schema.sql**
```sql
	drop table if exists item CASCADE;
    create table item
    (
        id        bigint generated by default as identity,
        item_name varchar(10),
        price     integer,
        quantity  integer,
        primary key (id)
    );
```
- 스프링 부트는 SQL 스크립트를 실행해서 애플리케이션 로딩 시점에 데이터베이스를 초기화하는 기능을 제공한다.

#### 스프링 부트와 임베디드 모드
- 스프링 부트는 임베디드 데이터베이스에 대한 설정도 기본으로 제공한다. 스프링부트는 데이터베이스에 대한 별다른 설정이 없으면 임베디드 데이터베이스를 사용한다.
```
 	#spring.datasource.url=jdbc:h2:tcp://localhost/~/testcase
  	#spring.datasource.username=sa
```
- src/test/resources/application.properties 에서 분리했던 DB 정보를 지우면 데이터베이스에 접근하는 모든 설정 정보가 사라지게 된다.
- 이렇게 별다른 설정 정보가 없으면 스프링 부트는 **임베디드 모드로 접근하는 DataSource**를 만들어서 제공한다.

## 데이터 접근 기술 - MyBatis
- JdbcTemplate보다 더 많은 기능을 제공하는 **SQL Mapper**이다. 기본적으로 JdbcTemplate이 제공하는 대부분의 기능을 제공하며 SQL을 XML에 작성할 수 있어서 **동적 쿼리 작성에 매우 편리**하다.

#### JdbcTemplate SQL
```java
 	String sql = "update item " +
          	"set item_name=:itemName, price=:price, quantity=:quantity " +
          	"where id=:id";
```

#### MyBatis SQL
```html
	<update id="update">
		update item
      	set item_name=#{itemName},
			price=#{price},
          	quantity=#{quantity}
      	where id = #{id}
	</update>
```

#### JdbcTemplate 동적 쿼리
```java
	String sql = "select id, item_name, price, quantity from item"; //동적 쿼리
  	if (StringUtils.hasText(itemName) || maxPrice != null) {
      	sql += " where";
	}
  	
    boolean andFlag = false;
  	if (StringUtils.hasText(itemName)) {
      	sql += " item_name like concat('%',:itemName,'%')";
      	andFlag = true;
  	}
  
  	if (maxPrice != null) {
      	if (andFlag) {
          	sql += " and";
      	}
      	sql += " price <= :maxPrice";
  	}
  
  	log.info("sql={}", sql);
  	return template.query(sql, param, itemRowMapper());
```

#### MyBatis 동적 쿼리
```html
	<select id="findAll" resultType="Item">
      	select id, item_name, price, quantity
      	from item
      	<where>
          	<if test="itemName != null and itemName != ''">
              	and item_name like concat('%',#{itemName},'%')
			</if>
            <if test="maxPrice != null">
                and price &lt;= #{maxPrice}
            </if>
        </where>
    </select>
```
- JdbcTemplate은 스프링에 내장된 기능이고 별도의 설정없이 사용할 수 있다는 장점이 있고 MyBatis는 약간의 설정이 필요하다.

### MyBatis 설정
#### application.properties
```
	mybatis.type-aliases-package=hello.itemservice.domain
  	mybatis.configuration.map-underscore-to-camel-case=true
```
- MyBatis에서 타입 정보를 사용할 때는 패키지 이름을 적어주어야 하는데, 여기에 미리 명시하면 패키지 이름을 생략할 수 있다.
- JdbcTempalte의 BeanPropertyRowMapper에서 처럼 언더바를 카멜로 자동 변경해주는 기능을 활성화한다.


#### MyBatis Mapper
```java
	@Mapper
  	public interface ItemMapper {
      	void save(Item item);
      	void update(@Param("id") Long id, @Param("updateParam") ItemUpdateDto updateParam);
      	Optional<Item> findById(Long id);
      	List<Item> findAll(ItemSearchCond itemSearch);
  	}
```
- MyBatis 매핑 XML을 호출해주는 매퍼 인터페이스이다.
- @Mapper 어노테이션을 붙임으로써 MyBatis에서 인식할 수 있다.
- 이 인터페이스의 메서드를 호출하면 XML에 작성한 SQL을 실행하고 결과를 반환한다.

#### src/main/resources/hello/itemservice/repository/mybatis/ItemMapper.xml
- 자바 코드가 아니기에 src/main/resources 하위에 만들되, 패키지의 위치를 맞춰야 한다.

```html
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="hello.itemservice.repository.mybatis.ItemMapper">
```
- namespace : 매퍼 인터페이스 지정
```xml
    <insert id ="save" useGeneratedKeys="true" keyProperty="id">
        insert into item (item_name, price, quantity)
        values (#{itemName}, #{price}, #{quantity})
    </insert>
```
- Insert SQL은 ```<insert>```를 사용하고 id에는 매퍼 인터페이스에 설정한 메서드 이름을 지정한다.
- 파라미터는 #{} 문법을 사용한다. 메서드에서 넘긴 객체의 **프로퍼티 이름**을 사용한다.
- #{} 문법은 PreparedStatement를 사용한다. JDBC의 ?를 치환하는 것과 같다.

```xml
    <update id="update">
        update item
        set item_name=#{updateParam.itemName},
            price=#{updateParam.price},
            quantity=#{updateParam.quantity}
        where id =#{id}
    </update>
```
- Update SQL은 ```<update>```를 사용한다.
- 파라미터가 2개이므로 메서드에서 **@Param** 어노테이션을 각각 붙여주어야 한다.

```html
    <select id="findById" resultType="Item">
        select id, item_name, price, quantity
        from item
        where id = #{id}
    </select>
```
- Select SQL은 ```<select>```를 사용한다.
- 반환 타입이 필요할 때는 resultType에 타입을 명시한다. 앞서 application.properties에서 설정했기 때문에 Item 타입만 작성했지만 설정하지 않았다면 패키지 명을 모두 작성해야 한다.
- JdbcTemplate의 BeanPropertyRowMapper처럼 결과를 객체로 바로 변환해준다.

```html
    <select id="findAll" resultType="Item">
        select id, item_name, price, quantity
        from item
        <where>
            <if test="itemName != null and itemName != ''">
                and item_name like concat('%',#{itemName},'%')
            </if>
            <if test="maxPrice != null">
                and price &lt;= #{maxPrice}
            </if>
        </where>
    </select>
</mapper>
```
- MyBatis는 <\where>, <\if> 동적 쿼리 문법을 통해 동적 쿼리 작성을 지원한다.
- XML에서는 <, \> 와 같은 특수문자를 사용할 수 없기 때문에 비교 조건 작성 방법이 다르다.

### 데이터 접근 계층 구현체 - MyBatis
```java
@Repository
@RequiredArgsConstructor
public class MyBatisItemRepository implements ItemRepository {

    private final ItemMapper itemMapper;

    @Override
    public Item save(Item item) {
        itemMapper.save(item);
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        itemMapper.update(itemId, updateParam);
    }

    @Override
    public Optional<Item> findById(Long id) {
        return itemMapper.findById(id);
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        return itemMapper.findAll(cond);
    }
}
```
- ItemRepository 를 구현해서 MyBatisItemRepository 를 만들자.
- MyBatisItemRepository 는 단순히 ItemMapper 에 기능을 위임한다.

### MyBatis 동작 원리
![](https://velog.velcdn.com/images/bon0057/post/f00954fe-797e-4c9c-a897-90eb5ca88faf/image.png)
1. **애플리케이션 로딩 시점**에 MyBatis 스프링 연동 모듈은 **@Mapper**가 붙어 있는 인터페이스를 조사한다.
2. 해당 인터페이스가 발견되면 **동적 프록시 기술**을 사용해서 Mapper 인터페이스의 구현체를 만든다.
3. 생성된 **구현체를 스프링 빈으로 등록**한다.

```
itemMapper class=class com.sun.proxy.$Proxy66
```
- 주입받아 사용한 ItemMapper의 클래스를 확인해보면 동적 프록시 기술이 적용된 것을 알 수 있다.

### Mapper 구현체
- MyBatis 스프링 연동 모듈이 만들어주는 Mapper의 구현체 덕분에 인터페이스만으로 XML의 SQL을 쉽게 호출할 수 있다.
- Mapper 구현체는 예외 변환까지 처리해준다. 스프링 예외 추상화를 지원한다.


### MyBatis 동적 쿼리 매뉴얼
[MyBatis 매뉴얼](https://mybatis.org/mybatis-3/ko/dynamic-sql.html)

## 데이터 접근 기술 - JPA
- 스프링과 JPA는 자바 엔터프라이즈(기업) 시장의 주력 기술이다.
- 스프링이 DI 컨테이너를 포함한 애플리케이션 전반의 다양한 기능을 제공한다면, JPA는 ORM 데이터 접근 기술을 제공한다.
- 실무에서는 JPA를 더욱 편리하게 사용하기 위해 스프링 데이터 JPA와 Querydsl이라는 기술을 함께 사용한다.

#### JPA 로그 확인
```
	logging.level.org.hibernate.SQL=DEBUG
  	logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

### 객체 - 테이블 ORM 매핑
```java
@Data
@Entity
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", length = 10)
    private String itemName;
    private Integer price;
    private Integer quantity;

    public Item() {
    }

    public Item(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }
}
```
- @Entity : JPA가 사용하는 객체를 나타낸다. 이 어노테이션이 있어야 JPA가 인식할 수 있고 DB에 존재하는 같은 이름(이름이 다를 경우 지정 가능)의 테이블과 매핑된다.
- @Id : 테이블의 Primary Key와 해당 필드를 매핑한다.
- @GeneratedValue(strategy = GenerationType.IDENTITY) : PK 값 생성을 데이터베이스에 맡기는 방식이다. **IDENTITY** 방식은 Auto Increment 방식이다.
- @Column : 객체의 필드를 테이블의 컬럼과 매핑한다. name을 통해 필드와 컬럼의 이름이 다를 경우 지정할 수 있고 생략할 경우 이름이 같아야 한다. 스프링 부트와 통합해서 사용하면 카멜 케이스와 언더스코어 방식을 자동으로 변환해준다.
- JPA는 public 또는 protected 의 기본 생성자가 필수이다.

### 데이터 접근 계층 구현체 - JPA
```java
@Slf4j
@Transactional
@Repository
public class JpaItemRepository implements ItemRepository {
    private final EntityManager em;

    public JpaItemRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public Item save(Item item) {
        em.persist(item);
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = em.find(Item.class, itemId);
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
    }

    @Override
    public Optional<Item> findById(Long id) {
        Item item = em.find(Item.class, id);
        return Optional.ofNullable(item);
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String jpql = "select i from Item i ";

        Integer maxPrice = cond.getMaxPrice();
        String itemName = cond.getItemName();

        if (StringUtils.hasText(itemName) || maxPrice != null) {
            jpql += " where";
        }

        boolean andFlag = false;

        if (StringUtils.hasText(itemName)) {
            jpql += " i.itemName like concat('%',:itemName,'%')";
            andFlag = true;
        }
        if (maxPrice != null) {
            if (andFlag) {
                jpql += " and";
            }
            jpql += " i.price <= :maxPrice";
        }

        log.info("jpql={}", jpql);
        TypedQuery<Item> query = em.createQuery(jpql, Item.class);

        if (StringUtils.hasText(itemName)) {
            query.setParameter("itemName", itemName);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }

        return query.getResultList();
    }
}
```
- private final EntityManager em : 스**프링 부트가 자등으로 등록한 엔티티 매니저를 주입받아 사용**한다. JPA의 동작은 엔티티 매니저를 통해서 이루어지며 엔티티 매니저는 내부에 데이터 소스를 갖고 있고 DB에 접근할 수 있다.
- @Transactional : **JPA의 모든 데이터 변경은 트랜잭션 안에서** 이루어져야 한다. 트랜잭션 안에 있지 않으면 오류가 발생한다. 여기서 레포지토리에 트랜잭션이 걸려있지만 일반적으로 비즈니스 로직을 시작하는 서비스 계층에 트랜잭션을 건다.
- JPA를 설정하려면 EntityManagerFactory, JpaTransactionManager, 데이터 소스 등 다양한 설정을 해야 하지만 **스프링 부트가 자동으로 설정**해준다.

#### save - 저장
```java
	public Item save(Item item) {
      	em.persist(item);
      	return item;
}
```
- JPA에서 객체를 테이블에 저장할 때는 엔티티 매니저가 제공하는 persist(객체) 메서드를 사용한다.
- JPA에서 자동으로 Insert SQL Query를 만들어서 DB에 저장한다.

#### update - 변경
```java
	public void update(Long itemId, ItemUpdateDto updateParam) {
      	Item findItem = em.find(Item.class, itemId);
      	findItem.setItemName(updateParam.getItemName());
      	findItem.setPrice(updateParam.getPrice());
      	findItem.setQuantity(updateParam.getQuantity());
}
```
- 변경을 위해 update() 메서드를 호출하지 않았다. JPA는 **트랜잭션이 커밋되는 시점에 변경된 엔티티 객체가 있는지 확인하고 특정 엔티티 객체가 변경된 경우에 Update SQL Query를 실행**한다.
- JPA는 변경된 엔티티 객체를 찾을 때 영속성 컨텍스트(1차 캐시)를 사용한다.

#### 단건 조회
```java
	public Optional<Item> findById(Long id) {
       	Item item = em.find(Item.class, id);
        return Optional.ofNullable(item);
   	}
```
- JPA에서 엔티티 객체를 Primary Key를 기준으로 조회할 때는 find() 메서드를 사용한다.

#### 조건 조회
```java
	public List<Item> findAll(ItemSearchCond cond) {
        String jpql = "select i from Item i ";

        Integer maxPrice = cond.getMaxPrice();
        String itemName = cond.getItemName();

        if (StringUtils.hasText(itemName) || maxPrice != null) {
            jpql += " where";
        }

        boolean andFlag = false;

        if (StringUtils.hasText(itemName)) {
            jpql += " i.itemName like concat('%',:itemName,'%')";
            andFlag = true;
        }
        if (maxPrice != null) {
            if (andFlag) {
                jpql += " and";
            }
            jpql += " i.price <= :maxPrice";
        }

        log.info("jpql={}", jpql);
        TypedQuery<Item> query = em.createQuery(jpql, Item.class);

        if (StringUtils.hasText(itemName)) {
            query.setParameter("itemName", itemName);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }

        return query.getResultList();
    }
```
- JPA는 JPQL이라는 객체지향 쿼리 언어를 제공한다. 여러 데이터를 복잡한 조건으로 조회할 때 사용한다. SQL이 테이블을 대상으로 한다면, JPQL은 엔티티 객체를 대상으로 SQL을 실행한다.
- JPA에서도 동적 쿼리는 쉽게 해결할 수 없고 복잡하기에 **Querydsl**이라는 기술을 통해 보완한다.

#### 예외 변환
- 엔티티 매니저는 순수한 JPA 기술이고 스프링과 관련이 없다. 그래서 엔티티 매니저는 예외가 발생하면 JPA 관련 예외를 발생시킨다.
- JPA 예외를 스프링 예외 추상화로 변환해주는 것은 @Repository 어노테이션을 통해 해결한다.
- @Repository가 붙은 클래스는 컴포넌트 스캔의 대상이 되며 동시에 예외 변환 AOP의 적용 대상이 된다.
  ![](https://velog.velcdn.com/images/bon0057/post/a4ef8831-8a48-44cf-b6e1-c201f491b581/image.png)
- 결과적으로 리포지토리에 @Repository 애노테이션만 있으면 스프링이 예외 변환을 처리하는 AOP를 만들어준다.

### 데이터 접근 기술 - 스프링 데이터 JPA
- 스프링 데이터 JPA는 JPA를 편리하게 사용할 수 있도록 도와주는 라이브러리이다.

```java
	public interface ItemRepository extends JpaRepository<Item, Long> {
  	}
```
- JpaRepository 인터페이스를 인터페이스 상속 받고, 제네릭에 관리할 <엔티티, 엔티티ID>를 주면 JpaRepository가 제공하는 기본 CRUD 기능을 모두 사용할 수 있다.

![](https://velog.velcdn.com/images/bon0057/post/a2c3744c-6010-4bbf-b2ae-04b67f307fc4/image.png)
- JpaRepository 인터페이스만 상속받으면 스프링 데이터 JPA가 프록시 기술을 사용해서 구현 클래스를 만들어준다. 만든 구현 클래스의 인스턴스를 만들어 스프링 빈으로 등록한다. 개발자는 이를 사용하기만 하면 된다.

#### vs 순수 JPA

##### 순수 JPA

```java
	public List<Member> findByUsernameAndAgeGreaterThan(String username, int age) {
		return em.createQuery("select m from Member m where m.username = :username and m.age > :age")
				.setParameter("username", username)
                .setParameter("age", age)
                .getResultList();
 }
 ```

 	- 순수 JPA를 사용하면 직접 JPQL을 작성하고, 파라미터도 직접 바인딩 해야 한다. 


##### 스프링 데이터 JPA
 ```java
	public interface MemberRepository extends JpaRepository<Member, Long> {
		List<Member> findByUsernameAndAgeGreaterThan(String username, int age);
	}
```
- 스프링 데이터 JPA는 메서드 이름을 분석해서 필요한 JPQL을 만들고 실행한다.
- 쿼리 메서드 기능 대신 직접 JPQL을 사용하고 싶을 때는 @Query 어노테이션과 함께 JPQL을 직접 작성하면 된다.

#### 쿼리 메서드 기능
- 조회: find...By , read...By , query...By , get...By
- COUNT: count...By 반환타입 long
- EXISTS: exists...By 반환타입 boolean
- 삭제: delete...By , remove...By 반환타입 long
- DISTINCT: findDistinct , findMemberDistinctBy LIMIT: findFirst3, findFirst, findTop ,findTop3

### 데이터 접근 계층 구현체 - 스프링 데이터 JPA

```java
	public interface SpringDataJpaItemRepository extends JpaRepository<Item, Long> {
    	List<Item> findByItemNameLike(String itemName);

    	List<Item> findByPriceLessThanEqual(Integer price);

    	//쿼리 메서드
    	List<Item> findByItemNameLikeAndPriceLessThanEqual(String itemName, Integer price);

    	//쿼리 직접 실행
    	@Query("select i from Item i where i.itemName like :itemName and i.price <= :price")
    	List<Item> findItems(@Param("itemName") String itemName, @Param("price") Integer price);
}
```
- JpaRepository 인터페이스를 상속받는 인터페이스를 만들면 자동으로 구현 클래스가 생성되고 인스턴스가 생성되어 스프링 빈으로 등록된다.

```java
@Repository
@Transactional
@RequiredArgsConstructor
public class JpaItemRepositoryV2 implements ItemRepository {

    private final SpringDataJpaItemRepository repository;

    @Override
    public Item save(Item item) {
        return repository.save(item);
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = repository.findById(itemId).orElseThrow();
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
    }

    @Override
    public Optional<Item> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        if (StringUtils.hasText(itemName) && maxPrice != null) {
            return repository.findItems("%" + itemName + "%", maxPrice);
        } else if (StringUtils.hasText(itemName)) {
            return repository.findByItemNameLike("%" + itemName + "%");
        } else if (maxPrice != null) {
            return repository.findByPriceLessThanEqual(maxPrice);
        } else {
            return repository.findAll();
        }
    }
}
```
- 스프링 빈으로 등록되어 있는 스프링 데이터 JPA 구현 클래스를 주입받아 사용한다.
- 스프링 데이터 JPA도 Example 이라는 기능으로 약간의 동적 쿼리를 지원하지만, 실무에서 사용하기는
  기능이 빈약하다. 실무에서 JPQL 동적 쿼리는 Querydsl을 사용하는 것이 좋다.

## 데이터 접근 기술 - Querydsl
- JPA, MongoDB, SQL 같은 기술들을 위해 type-safe SQL을 만드는 프레임워크

### 데이터 접근 계층 구현체 - Querydsl
```java
@Slf4j
@Repository
@Transactional
public class JpaItemRepositoryV3 implements ItemRepository {

    private final EntityManager em;
    private final JPAQueryFactory query;

    public JpaItemRepositoryV3(EntityManager em) {
        this.em = em;
        this.query = new JPAQueryFactory(em);
    }
    @Override
    public Item save(Item item) {
        em.persist(item);
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = findById(itemId).orElseThrow();
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
    }

    @Override
    public Optional<Item> findById(Long id) {
        Item item = em.find(Item.class, id);
        return Optional.ofNullable(item);
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        QItem item = QItem.item;
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(itemName)) {
            builder.and(item.itemName.like("%" + itemName + "%"));
        }
        if (maxPrice != null) {
            builder.and(item.price.loe(maxPrice));
        }

        List<Item> result = query.select(item)
                .from(item)
                .where(builder)
                .fetch();

        return result;
    }
}
```
- Querydsl을 사용하려면 JPAQueryFactory 가 필요하다. JPAQueryFactory 는 JPA 쿼리인 JPQL을 만들기 때문에 EntityManager 가 필요하다.
- Querydsl 덕분에 동적 쿼리를 매우 깔끔하게 사용할 수 있다.
- 쿼리 문장에 오타가 있어도 컴파일 시점에 오류를 막을 수 있다.
- 구체적인 Querydsl 사용법과 문법은 추후에 김영한님의 Querydsl 강의를 듣고 정리할 것.

## 데이터 접근 기술 - 스프링 데이터 JPA & Querydsl
![](https://velog.velcdn.com/images/bon0057/post/8d7f7ab6-da3b-42fb-8747-ce83952decc3/image.png)
- 스프링 데이터 JPA 기능을 제공하는 레퍼지토리와 Querydsl을 사용해서 복잡한 쿼리 기능을 제공하는 레포지토리 2개를 모두 사용하여 기본 CRUD와 단순 조회는 스프링 데이터 JPA가 담당하고, 복잡한 조회 커리는 Querydsl이 담당하게 한다.

### 스프링 데이터 JPA 레퍼지토리
```java
public interface ItemRepositoryV2 extends JpaRepository<Item, Long> {
}
```
### Querydsl 레퍼지토리
```java
@Repository
public class ItemQueryRepositoryV2 {

    private final JPAQueryFactory query;

    public ItemQueryRepositoryV2(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        QItem item = QItem.item;
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(itemName)) {
            builder.and(item.itemName.like("%" + itemName + "%"));
        }
        if (maxPrice != null) {
            builder.and(item.price.loe(maxPrice));
        }

        List<Item> result = query.select(item)
                .from(item)
                .where(builder)
                .fetch();

        return result;
    }
}
```

### 서비스 계층 구현체
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceV2 implements ItemService {

    private final ItemRepositoryV2 itemRepositoryV2;
    private final ItemQueryRepositoryV2 itemQueryRepositoryV2;
    
    @Override
    public Item save(Item item) {
        return itemRepositoryV2.save(item);
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = itemRepositoryV2.findById(itemId).orElseThrow();
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
    }

    @Override
    public Optional<Item> findById(Long id) {
        return itemRepositoryV2.findById(id);
    }

    @Override
    public List<Item> findItems(ItemSearchCond cond) {
        return itemQueryRepositoryV2.findAll(cond);
    }
}
```
- ItemRepositoryV2 는 스프링 데이터 JPA의 기능을 제공하는 리포지토리이다.
- ItemQueryRepositoryV2 는 Querydsl을 사용해서 복잡한 쿼리 기능을 제공하는 리포지토리이다.
- 이렇게 둘을 분리하면 기본 CRUD와 단순 조회는 스프링 데이터 JPA가 담당하고, 복잡한 조회 쿼리는 Querydsl이 담당하게 된다.
- 데이터 접근 기술 선택에는 정답이 없다. JdbcTemplate이나 MyBatis같이 SQL Mapper들은 SQL을 직접 작성해야 하지만 기술이 단순하다. JPA, 스프링 데이터 JPA, Querydsl 같은 기술들은 개발 생산성을 혁신할 수 있지만 기술이 복잡하고 알아야 할 것이 많다.

### 주의점
- JPA, 스프링 데이터 JPA, Querydsl은 모두 JPA 기술을 사용하기에 트랜잭션 매니저로 JpaTransactionManager를 사용해야 하고 JdbcTemplate, MyBatis 기술은 내부에서 JDBC를 직접 사용해 DataSourceTransactionManager를 사용한다. 두 기술을 사용하면 트랜잭션 매니저가 달라지지만 JpaTransactionManager도 DataSourceTransactionManager를 제공하기에 동시에 사용할 수 있다.
- 두 기술을 동시에 사용할 때 JPA는 데이터를 변경하는 경우 즉시 데이터베이스에 반영하는 것이 아닌 플러시 타임(보통 커밋과 동시)에 반영되기에 JPA를 사용하여 데이터를 변경한 후 JdbcTemplate을 사용하는 경우 데이터를 찾지 못할 수 있기에 조심해야 한다.


## 스프링 트랜잭션
- 스프링은 PlatformTransactionManager라는 인터페이스를 통해 트랜잭션을 추상화한다.
```java
	public interface PlatformTransactionManager extends TransactionManager { TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException;
		void commit(TransactionStatus status) throws TransactionException;
      	void rollback(TransactionStatus status) throws TransactionException;
	}
```
![](https://velog.velcdn.com/images/bon0057/post/64ed6267-a05c-4970-9adf-212d381c1d66/image.png)

- 스프링은 트랜잭션 추상화와 더불어 데이터 접근 기술에 대한 트랜잭션 매니저의 구현체도 제공한다. 개발자는 필요한 구현체를 스프링 빈으로 등록하고 주입 받아서 사용하면 된다.
- 스프링 부트는 어떤 데이터 접근 기술을 사용하는지를 자**동으로 인식해 적절한 트랜잭션 매니저를 선택해 스프링 빈으로 등록**해준다.

### 선언적 트랜잭션 & AOP
- @Transactional을 통한 선언적 트랜잭션 관리 방식을 사용하게 되면 기본적으로 프록시 방식의 AOP가 적용된다.
  ![](https://velog.velcdn.com/images/bon0057/post/4e17d3c1-4b92-4d5a-9f7a-2be8e3e31586/image.png)
- 트랜잭션은 커넥션에 con.setAutocommit(false)를 지정하면서 시작한다.
- 같은 트랜잭션을 유지하려면 같은 데이터베이스 커넥션을 사용해야 한다. 이를 위해 스프링 내부에서는 트랜잭션 동기화 매니저가 사용된다.

### 테스트
```java
@SpringBootTest
public class TxBasicTest {
	@Autowired
	BasicService basicService;
      
	@Test
    void proxyCheck() {
		log.info("aop class={}", basicService.getClass());
        assertThat(AopUtils.isAopProxy(basicService)).isTrue();
    }

	@Test
    void txTest() {
        basicService.tx();
        basicService.nonTx();
    }
    
    @TestConfiguration
    static class TxApplyBasicConfig {
        @Bean
        BasicService basicService() {
            return new BasicService();
        }
	}

	@Slf4j
    static class BasicService {
        @Transactional
        public void tx() {
            log.info("call tx");
            boolean txActive =
TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
        }
        public void nonTx() {
            log.info("call nonTx");
            boolean txActive =
TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
		} 
	}
```
- AopUtils.isAopProxy() : 선언적 트랜잭션 방식에서 스프링 트랜잭션은 AOP를 기반으로 동작한다. @Transactional을 메서드나 클래스에 붙이면 해당 객체는 트랜잭션 AOP 적용의 대상이 되고 실제 객체 대신 트랜잭션을 처리해주는 프록시 객체가 스프링 빈에 등록되고 주입받을 때도 실체 객체 대신 프록시 객체가 주입된다.
- 클라이언트가 basicService.tx()를 호출하면 트랜잭션을 시작하며 프록시 객체의 tx()가 호출된다.
- basicService.nontx()를 호출하면 @Transactional 어노테이션이 없으므로 트랜잭션을 시작하지 않고 호출만 하고 종료한다.

### 트랜잭션 적용 위치
- 스프링에서 우선순위는 항상 더 구체적이고 자세한 것이 높은 우선순위를 가진다. 이것만 기억하면 스프링에서 발생하는 대부분의 우선순위를 쉽게 기억할 수 있다. 그리고 더 구체적인 것이 더 높은 우선순위를 가지는 것은 상식적으로 자연스럽다. 예를 들어서 메서드와 클래스에 애노테이션을 붙일 수 있다면 더 구체적인 메서드가 더 높은 우선순위를 가진다.
- 스프링의 @Transactional 은 다음 두 가지 규칙이 있다.
    - 우선순위 규칙
    - 클래스에 적용하면 메서드는 자동 적용
#### 테스트
```java
@SpringBootTest
public class TxLevelTest {

    @Autowired
    LevelService service;

    @Test
    void orderTest() {
        service.write();
        service.read();
    }

    @TestConfiguration
    static class TxLevelTestConfig {
        @Bean
        LevelService levelService() {
            return new LevelService();
        }
    }

    @Slf4j
    @Transactional(readOnly = true)
    static class LevelService {

        @Transactional(readOnly = false)
        public void write() {
            log.info("call write");
            printTxInfo();
        }

        public void read() {
            log.info("call read");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active ={}", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx readOnly ={}", readOnly);
        }
    }
}
```
- write() : 클래스 보다는 메서드가 더 구체적이므로 메서드에 있는 @Transactional(readOnly = false) 옵션을 사용한 트랜잭션이 적용된다.
- read() : 클래스에 @Transactional(readOnly = true) 이 적용되어 있다. 따라서 트랜잭션이 적용되고 readOnly = true 옵션을 사용하게 된다.


### 주의 사항 - 내부 호출
- @Transactional을 사용하면 스프링의 트랜잭션 AOP가 적용된다. 메서드를 호출하면 프록시 객체가 요청을 먼저 받아서 트랜잭션을 처리하고 실제 객체를 호출한다. **트랜잭션을 적용하려면 항상 프록시를 통해서 대상 객체를 호출**해야 한다.
- 스프링은 의존관계 주입시에 실제 객체 대신 프록시 객체를 주입하기 때문에 객체를 직접 호출하는 문제는 일반적으로는 없지만 대상 **객체의 내부에서 메서드 호출이 발생하면 프록시 객체를 거치지 않고 대상 객체를 직접 호출하는 문제가 발생**한다.

```java
@Slf4j
@SpringBootTest
public class InternalCallV1Test {

    @Autowired
    CallService callService;

    @Test
    void printProxy() {
        log.info("callService class={}", callService.getClass());
    }
    
    @Test
	void internalCall() {
    	callService.internal();
	}

    @Test
    void externalCall() {
        callService.external();
    }

    @TestConfiguration
    static class InternalCallV1TestConfig {

        @Bean
        CallService callService() {
            return new CallService();
        }
    }


    @RequiredArgsConstructor
    static class CallService {

        public void external() {
            log.info("call external");
            printTxInfo();
            internal();
        }
        
        @Transactional
    	public void internal() {
        	log.info("call internal");
        	printTxInfo();
    	}

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active ={}", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx readOnly ={}", readOnly);
        }
    }
}
```
![](https://velog.velcdn.com/images/bon0057/post/5c2a3bfb-cf04-4605-957e-0108e259ff68/image.png)

- callService 클래스의 internal 메서드에 @Transactional 어노테이션이 붙어 있으므로 callService의 프록시 객체가 스프링 빈으로 등록되고 이후에 주입할 때 사용된다.
- callService.internal()을 호출하면 callService는 프록시 객체이고 internal 메서드에 @Transactional 어노테이션이 있으므로 프록시 객체에서 우선 트랜잭션을 적용 후 실제 callService 객체 인스턴스의 internal() 메서드를 호출한다.
- callService.external()을 호출하면 callService는 프록시 객체이지만 external 메서드에 @Transactional 어노테이션이 없으므로 **트랜잭션을 적용하지 않고 실제 callService 객체 인스턴스의 external() 메서드를 호출**한다. external()은 내부에서 internal() 메서드를 호출하는데 이때 자기 자신의 내부 메서드인 internal()을 호출한다. 결과적으로 프록시 객체를 거치지 않으므로 트랜잭션이 적용되지 않는다.
#### 문제 원인
- 자바 언어에서 메서드 앞에 별도의 참조가 없으면 this 라는 뜻으로 자기 자신의 인스턴스를 가리킨다. 결과적으로 자기 자신의 내부 메서드를 호출하는 this.internal() 이 되는데, 여기서 this 는 자기 자신을 가리키므로, 실제 대상 객체(target)의 인스턴스를 뜻한다. 결과적으로 이러한 내부 호출은 프록시를 거치지 않는다. 따라서 트랜잭션을 적용할 수 없다. 결과적으로 target 에 있는 internal() 을 직접 호출하게 된 것이다.

#### 문제 해결 - 내부 호출 -> 외부 호출
```java
@Slf4j
@SpringBootTest
public class InternalCallV2Test {

    @Autowired
    CallService callService;

    @Test
    void printProxy() {
        log.info("callService class={}", callService.getClass());
    }

    @Test
    void externalCall() {
        callService.external();
    }

    @TestConfiguration
    static class InternalCallV1TestConfig {

        @Bean
        CallService callService() {
            return new CallService(internalService());
        }

        @Bean
        InternalService internalService() {
            return new InternalService();
        }
    }


    @RequiredArgsConstructor
    static class CallService {

        private final InternalService internalService;

        public void external() {
            log.info("call external");
            printTxInfo();
            internalService.internal();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active ={}", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx readOnly ={}", readOnly);
        }
    }

    static class InternalService {
        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active ={}", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx readOnly ={}", readOnly);
        }
    }


}
```
![](https://velog.velcdn.com/images/bon0057/post/7b8410bd-564c-4f30-bf12-cfec4719f27a/image.png)
-  클라이언트인 테스트 코드는 callService.external() 을 호출한다.
- callService 는 실제 callService 객체 인스턴스이다.
- callService 는 주입 받은 internalService.internal() 을 호출한다.
- internalService 는 트랜잭션 프록시이다. internal() 메서드에 @Transactional 이 붙어 있으므로 트랜잭션 프록시는 트랜잭션을 적용한다.
- 트랜잭션 적용 후 실제 internalService 객체 인스턴스의 internal() 을 호출한다.

### 주의 사항 - 초기화 시점
```java
@SpringBootTest
public class InitTxTest {

    @Autowired
    Hello hello;

    @Test
    void go() {
        // 초기화 코드는 스프링이 초기화 시점에 호출
        hello.initV1();
    }

    @TestConfiguration
    static class InitTxConfig {

        @Bean
        Hello hello() {
            return new Hello();
        }
    }


    @Slf4j
    static class Hello {

        @PostConstruct
        @Transactional
        public void initV1() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello init @PostConstruct tx active={}", isActive);
        }

        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void initV2() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello init ApplicationReady tx active={}", isActive);
        }
    }
}
```
- 초기화 코드가 먼저 호출되고, 그 다음에 트랜잭션 AOP가 적용되기 때문에 초기화 시점에는 해당 메서드에서 트랜잭션을 획득할 수 없다.

### 트랜잭션 옵션
```java
	@Transactional("memberTxManager")
      	public void member() {...}
```
- value, transactionManager : 이 값을 생략하면 기본으로 등록된 트랜잭션 매니저를 사용하기 때문에 대부분 생략하나 사용하는 트랜잭션 매니저가 둘 이상이라면 옵션을 통해 지정해서 사용하면 된다.
```java
	@Transactional(rollbackFor = Exception.class)
```
- rollbackFor : 예외 발생시 스프링 트랜잭션의 기본 정책은 언체크 예외가 발생하면 롤백, 체크 예외가 발생하면 커밋한다. 이 옵션을 사용하면 기본 정책에 추가로 어떤 예외가 발생할 때 롤백할 지 지정할 수 있다.
- noRollbackFor
- propagation
- isolation
- timeout
- label
- readOnly

### 예외 + 트랜잭션 커밋, 롤백

#### 롤백 테스트
```java
@SpringBootTest
public class RollbackTest {

    @Autowired
    RollbackService rollbackService;

    @Test
    void runtimeException() {
        Assertions.assertThatThrownBy(() -> rollbackService.runtimeException())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void checkedException() {
        Assertions.assertThatThrownBy(() -> rollbackService.checkedException())
                .isInstanceOf(MyException.class);
    }

    @Test
    void rollbackFor() {
        Assertions.assertThatThrownBy(() -> rollbackService.rollbackFor())
                .isInstanceOf(MyException.class);
    }

    @TestConfiguration
    static class RollbackTestConfig {

        @Bean
        RollbackService rollbackService() {
            return new RollbackService();
        }
    }

    @Slf4j
    static class RollbackService {

        // 런타임 예외 발생 : 롤백
        @Transactional
        public void runtimeException() {
            log.info("call runtimeException");
            throw new RuntimeException();
        }

        // 체크 예외 발생 : 커밋
        @Transactional
        public void checkedException() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }

        // 체크 예외 발생 : 롤백
        @Transactional(rollbackFor = MyException.class)
        public void rollbackFor() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }
    }

    static class MyException extends Exception {
    }


}
```
- runtimeException() : RuntimeException이 발생하면 트랜잭션이 롤백된다.
- checkedException() : 체크 예외가 발생하면 트랜잭션이 커밋된다.
- rollbackFor() : 체크 예외가 발생했음에도 rollbackFor 옵션을 지정했다면 롤백할 수 있다.
- 스프링 기본적으로 체크 예외는 비즈니스 의미가 있을 때 사용하고, 런타임(언체크) 예외는 복구 불가능한 예외로 가정한다.
    - 체크 예외: 비즈니스 의미가 있을 때 사용
    - 언체크 예외: 복구 불가능한 예외



## 트랜잭션 전파
- 트랜잭션이 이미 진행중일 때 추가로 트랜잭션을 수행하면 어떻게 동작할지 결정하는 것을 트랜잭션 전파라고 한다.
  ![](https://velog.velcdn.com/images/bon0057/post/32a7fe5e-0a14-4311-ba9d-38d1efbad671/image.png)
- 외부 트랜잭션이 수행중이고, 아직 끝나지 않았는데, 내부 트랜잭션이 수행된다.
- 외부 트랜잭션이라고 이름 붙인 것은 둘 중 상대적으로 밖에 있기 때문에 외부 트랜잭션이라 한다. 처음 시작된 트랜잭션으로 이해하면 된다.
- 내부 트랜잭션은 외부에 트랜잭션이 수행되고 있는 도중에 호출되기 때문에 마치 내부에 있는 것 처럼 보여서 내부 트랜잭션이라 한다.
  ![](https://velog.velcdn.com/images/bon0057/post/0e178146-613d-4945-ab60-134e17972293/image.png)
- 스프링 이 경우 외부 트랜잭션과 내부 트랜잭션을 묶어서 하나의 트랜잭션을 만들어준다. 내부 트랜잭션이 외부 트랜잭션에 참여하는 것이다. 이것이 기본 동작이고, 옵션을 통해 다른 동작방식도 선택할 수 있다.

![](https://velog.velcdn.com/images/bon0057/post/3ac1e95e-ff4e-4210-aab7-45473f041157/image.png)
- 스프링은 트랜잭션을 물리 트랜잭션, 논리 트랜잭션으로 나눈다. 논리 트랜잭션은 하나의 물리 트랜잭션에 포함된다.
- 물리 트랜잭션은 실제 데이터베이스에 적용되는 트랜잭션을 뜻하며 실제 커넥션을 통해서 커밋, 롤백하는 단위이다.
- 논리 트랜잭션은 트랜잭션 매니저를 통해 트랜잭션을 사용하는 단위이다.
- 트랜잭션이 사용중일 때 또 다른 트랜잭션이 내부에 사용되면 여러가지 복잡한 상황이 발생한다. 이때 논리 트랜잭션 개념을 도입하면 다음과 같은 단순한 원칙을 만들 수 있다.

### 전파 기본 원칙
- 모든 논리 트랜잭션이 커밋되어야 물리 트랜잭션이 커밋된다.
- 하나의 논리 트랜잭션이라도 롤백되면 물리 트랜잭션은 롤백된다.
  ![](https://velog.velcdn.com/images/bon0057/post/dbd0f9c9-c9ee-4959-b715-724ce22500d2/image.png)

### 전파 예제 - 내부 커밋, 외부 커밋
![](https://velog.velcdn.com/images/bon0057/post/2288b1a9-bf55-4177-bb94-bd6bb0643af4/image.png)

```java
	@Test
  	void inner_commit() {
		log.info("외부 트랜잭션 시작");
      	TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
      	log.info("outer.isNewTransaction()={}", outer.isNewTransaction());
        
		log.info("내부 트랜잭션 시작");
      	TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
		log.info("inner.isNewTransaction()={}", inner.isNewTransaction()); 				log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);
        
		log.info("외부 트랜잭션 커밋");
      	txManager.commit(outer);
  }
```
- 내부 트랜잭션을 시작할 때 외부 트랜잭션이 진행중이므로 외부 트랜잭션에 참여한다.
- 외부 트랜잭션은 처음 수행된 트랜잭션이다. 이 경우 신규 트랜잭션( isNewTransaction=true )이 된다. 내부 트랜잭션을 시작하는 시점에는 이미 외부 트랜잭션이 진행중인 상태이다. 이 경우 내부 트랜잭션은 외부 트랜잭션에 참여한다.
- 외부 트랜잭션과 내부 트랜잭션이 하나의 물리 트랜잭션으로 묶이는 것이다.
  내부 트랜잭션은 이미 진행중인 외부 트랜잭션에 참여한다. 이 경우 신규 트랜잭션이 아니다 ( isNewTransaction=false ).

#### 테스트 결과
```
	외부 트랜잭션 시작
  	Creating new transaction with name [null]:
  	PROPAGATION_REQUIRED,ISOLATION_DEFAULT
  	Acquired Connection [HikariProxyConnection@1943867171 wrapping conn0] for JDBC
  	transaction
  	Switching JDBC Connection [HikariProxyConnection@1943867171 wrapping conn0] to manual commit
  	outer.isNewTransaction()=true
	내부 트랜잭션 시작
	Participating in existing transaction inner.isNewTransaction()=false
	내부 트랜잭션 커밋
	외부 트랜잭션 커밋
  	Initiating transaction commit
  	Committing JDBC transaction on Connection [HikariProxyConnection@1943867171    wrapping conn0]
  	Releasing JDBC Connection [HikariProxyConnection@1943867171 wrapping conn0] 
    after transaction
```
- 내부 트랜잭션을 시작할 때 Participating in existing transaction 이라는 메시지를 확인할 수 있다. 이 메시지는 내부 트랜잭션이 기존에 존재하는 외부 트랜잭션에 참여한다는 뜻이다.
- 외부 트랜잭션을 시작하거나 커밋할 때는 DB 커넥션을 통한 물리 트랜잭션을 시작
  ( manual commit )하고, DB 커넥션을 통해 커밋 하는 것을 확인할 수 있다. 그런데 내부 트랜잭션을 시작하거나 커밋할 때는 DB 커넥션을 통해 커밋하는 로그를 전혀 확인할 수 없다.
- 외부 트랜잭션만 물리 트랜잭션을 시작하고, 커밋한다.
  만약 내부 트랜잭션이 실제 물리 트랜잭션을 커밋하면 트랜잭션이 끝나버리기 때문에, 트랜잭션을 처음 시작한 외부 트랜잭션까지 이어갈 수 없다. 따라서 내부 트랜잭션은 DB 커넥션을 통한 물리 트랜잭션을 커밋하면 안된다.
- 스프링은 이렇게 여러 트랜잭션이 함께 사용되는 경우, 처음 트랜잭션을 시작한 외부 트랜잭션이 실제 물리 트랜잭션을 관리하도록 한다. 이를 통해 트랜잭션 중복 커밋 문제를 해결한다.

#### 동작 방식
![](https://velog.velcdn.com/images/bon0057/post/3a40a509-142d-4b61-8bfa-bd646527c260/image.png)
![](https://velog.velcdn.com/images/bon0057/post/06b13f0a-762e-489e-9216-bc0a90d35b4e/image.png)
- 핵심은 트랜잭션 매니저에 커밋을 호출한다고해서 항상 실제 커넥션에 물리 커밋이 발생하지는 않는다는 점이다.
- 신규 트랜잭션인 경우에만 실제 커넥션을 사용해서 물리 커밋과 롤백을 수행한다. 신규 트랜잭션이 아니면 실제 물리 커넥션을 사용하지 않는다.
- 트랜잭션이 내부에서 추가로 사용되면 트랜잭션 매니저에 커밋하는 것이 항상 물리 커밋으로 이어지지 않는다. 그래서 이 경우 논리 트랜잭션과 물리 트랜잭션을 나누게 된다. 또는 외부 트랜잭션과 내부 트랜잭션으로 나누어 설명하기도 한다.

### 테스트 - 내부 커밋, 외부 롤백
![](https://velog.velcdn.com/images/bon0057/post/c290783b-a25a-49f3-a07d-899638a85c3f/image.png)
- 논리 트랜잭션이 하나라도 롤백되면 전체 물리 트랜잭션은 롤백된다.
  따라서 이 경우 내부 트랜잭션이 커밋했어도, 내부 트랜잭션 안에서 저장한 데이터도 모두 함께 롤백된다.

```java
	@Test
  	void outer_rollback() {
		log.info("외부 트랜잭션 시작");
      	TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
		
        log.info("내부 트랜잭션 시작");
      	TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
		log.info("내부 트랜잭션 커밋"); 
        txManager.commit(inner);

		log.info("외부 트랜잭션 롤백");
      	txManager.rollback(outer);
  	}
```
- 내부에서 커밋했을 때 신규 트랜잭션이 아니므로 실제 커밋을 호출하지 않고 외부(신규) 트랜잭션이 종료될 때까지 기다린다.

#### 테스트 결과
```
	외부 트랜잭션 시작
  	Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
  	Acquired Connection [HikariProxyConnection@461376017 wrapping conn0] for JDBC transaction
  	Switching JDBC Connection [HikariProxyConnection@461376017 wrapping conn0] to manual commit
	내부 트랜잭션 시작
  	Participating in existing transaction
	내부 트랜잭션 커밋
	외부 트랜잭션 롤백
  	Initiating transaction rollback
  	Rolling back JDBC transaction on Connection [HikariProxyConnection@461376017 wrapping conn0]
	Releasing JDBC Connection [HikariProxyConnection@461376017 wrapping conn0]
	after transaction
```
- 외부 트랜잭션이 물리 트랜잭션을 시작하고 롤백하는 것을 확인할 수 있다.
- 내부 트랜잭션은 앞서 배운대로 직접 물리 트랜잭션에 관여하지 않는다.
- 결과적으로 외부 트랜잭션에서 시작한 물리 트랜잭션의 범위가 내부 트랜잭션까지 사용된다. 이후 외부 트랜잭션이 롤백되면서 전체 내용은 모두 롤백된다.

#### 동작 방식
![](https://velog.velcdn.com/images/bon0057/post/06330395-14be-4a5f-8ca9-07e0081daa86/image.png)

### 테스트 - 내부 롤백, 외부 커밋
![](https://velog.velcdn.com/images/bon0057/post/3febcd1b-5628-49b5-afeb-a8c4cd1eada2/image.png)
```java
	@Test
  	void inner_rollback() {
		log.info("외부 트랜잭션 시작");
      	TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
		
        log.info("내부 트랜잭션 시작");
      	TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
		log.info("내부 트랜잭션 롤백"); 
        txManager.rollback(inner);
		
        log.info("외부 트랜잭션 커밋");
		assertThatThrownBy(() -> txManager.commit(outer))
        	.isInstanceOf(UnexpectedRollbackException.class);
	}		
```
- 내부 트랜잭션을 롤백하면 실제 물리 트랜잭션은 롤백하지 않는다. 대신 기존 트랜잭션을 롤백 전용(rollbackOnly)를 표시한다.
- 마지막에 외부 트랜잭션이 커밋할 때 롤백 전용으로 표시되어 있기에 UnexpectedRollbackException이 발생한다.
  ![](https://velog.velcdn.com/images/bon0057/post/cf127a1b-c173-470c-85f2-b89b53cf2a9c/image.png)

#### 테스트 결과
```
	외부 트랜잭션 시작
  	Creating new transaction with name [null]:  PROPAGATION_REQUIRED,ISOLATION_DEFAULT
  	Acquired Connection [HikariProxyConnection@220038608 wrapping conn0] for JDBC transaction
  	Switching JDBC Connection [HikariProxyConnection@220038608 wrapping conn0] to manual commit
	내부 트랜잭션 시작
  	Participating in existing transaction
	내부 트랜잭션 롤백
  	Participating transaction failed - marking existing transaction as rollback-only
  	Setting JDBC transaction [HikariProxyConnection@220038608 wrapping conn0] rollback-only
	외부 트랜잭션 커밋
  	Global transaction is marked as rollback-only but transactional code requested commit
  	Initiating transaction rollback
  	Rolling back JDBC transaction on Connection [HikariProxyConnection@220038608 wrapping conn0]
  	Releasing JDBC Connection [HikariProxyConnection@220038608 wrapping conn0]
  	after transaction
```
- 내부 트랜잭션 롤백 : Participating transaction failed - marking existing transaction as rollback-only
- 외부 트랜잭션을 커밋 : Global transaction is marked as rollback-only
- 커밋을 호출했지만, 전체 트랜잭션이 롤백 전용으로 표시되어 있다. 따라서 물리 트랜잭션을 롤백한다.

#### 동작 방식
![](https://velog.velcdn.com/images/bon0057/post/2766ba1f-2b8b-424f-8bdc-0a21b0eb2594/image.png)
- 트랜잭션 매니저에 커밋을 호출한 개발자 입장에서는 분명 커밋을 기대했는데 롤백 전용 표시로 인해 실제로는 롤백이 되어버렸다. 이것은 조용히 넘어갈 수 있는 문제가 아니다. 시스템 입장에서는 커밋을 호출했지만 롤백이 되었다는 것은 분명하게 알려주어야 한다.
- 예를 들어서 고객은 주문이 성공했다고 생각했는데, 실제로는 롤백이 되어서 주문이 생성되지 않은 것이다.
  스프링은 이 경우 UnexpectedRollbackException 런타임 예외를 던진다. 그래서 커밋을 시도했지만, 기대하지 않은 롤백이 발생했다는 것을 명확하게 알려준다.

### 테스트 - REQUIRES_NEW
- 외부 트랜잭션과 내부 트랜잭션을 완전히 분리해서 각각 별도의 물리 트랜잭션을 사용하는 방법이다. 그래서 커밋과 롤백도 각각 별도로 이루어지게 된다.
- 이 방법은 내부 트랜잭션에 문제가 발생해서 롤백해도, 외부 트랜잭션에는 영향을 주지 않는다. 반대로 외부 트랜잭션에 문제가 발생해도 내부 트랜잭션에 영향을 주지 않는다.
  ![](https://velog.velcdn.com/images/bon0057/post/3c4b3f70-4d33-4ac7-9f24-3d00a80b17a8/image.png)
```java
	@Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManger.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus inner = txManger.getTransaction(definition);
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());

        log.info("내부 트랜잭션 롤백");
        txManger.rollback(inner);

        log.info("외부 트랜잭션 커밋");
        txManger.commit(outer);
    }
```
- REQUIRES_NEW 전파 옵션을 사용하면 내부 트랜잭션을 시작할 때 기존 트랜잭션에 참여하는 것이 아닌 새로운 물리 트랜잭션을 만들어서 시작하게 된다.
- 외부 트랜잭션과 내부 트랜잭션이 각각 별도의 물리 트랜잭션을 가진다.
- 별도의 물리 트랜잭션을 가진다는 뜻은 DB 커넥션을 따로 사용한다는 뜻이다.
- 이때 외부 트랜잭션의 커넥션은 내부 트랜잭션의 커넥션이 종료될 때까지 보류된다.

#### 테스트 결과
```
	외부 트랜잭션 시작
  	Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
  	Acquired Connection [HikariProxyConnection@1064414847 wrapping conn0] for JDBC transaction
  	Switching JDBC Connection [HikariProxyConnection@1064414847 wrapping conn0] to manual commit
  	outer.isNewTransaction()=true
	내부 트랜잭션 시작
  	Suspending current transaction, creating new transaction with name [null]
  	Acquired Connection [HikariProxyConnection@778350106 wrapping conn1] for JDBC transaction
  	Switching JDBC Connection [HikariProxyConnection@778350106 wrapping conn1] to manual commit
  	inner.isNewTransaction()=true
   	내부 트랜잭션 롤백
	Initiating transaction rollback
  	Rolling back JDBC transaction on Connection [HikariProxyConnection@778350106 wrapping conn1]
  	Releasing JDBC Connection [HikariProxyConnection@778350106 wrapping conn1]
  	after transaction
  	Resuming suspended transaction after completion of inner transaction
	외부 트랜잭션 커밋
  	Initiating transaction commit
  	Committing JDBC transaction on Connection [HikariProxyConnection@1064414847 wrapping conn0]
  	Releasing JDBC Connection [HikariProxyConnection@1064414847 wrapping conn0]
  	after transaction
```

#### 동작 방식
![](https://velog.velcdn.com/images/bon0057/post/8355e92a-bed1-485f-9222-375893ef7ba4/image.png)

### 전파 옵션
- REQUIRED : 가장 많이 사용하는 기본 설정이다. 기존 트랜잭션이 없으면 생성하고, 있으면 참여한다.
- REQUIRED_NEW : 항상 새로운 트랜잭션을 생성한다.
- SUPPORT : 트랜잭션을 지원한다는 뜻이다. 기존 트랜잭션이 없으면, 없는대로 진행하고, 있으면 참여한다.
- NOT_SUPPORT : 트랜잭션을 지원하지 않는다는 의미
- MANDATORY : 의무사항이다. 트랜잭션이 반드시 있어야 한다. 기존 트랜잭션이 없으면 예외가 발생한다.
- NEVER : 트랜잭션을 사용하지 않는다는 의미이다. 기존 트랜잭션이 있으면 예외가 발생한다. 기존 트랜잭션도 허용하지 않는 강한 부정의 의미로 이해하면 된다.
- NESTED

### 전파 예제
#### 사용자 도메인
```java
@Entity
@Getter
@Setter
public class Member {

    @Id @GeneratedValue
    private Long id;
    private String username;

    public Member() {

    }

    public Member(String username) {
        this.username = username;
    }
}
```
#### 사용자 레퍼지토리
```java
@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final EntityManager em;

    @Transactional
    public void save(Member member) {
        log.info("member 저장");
        em.persist(member);
    }

    public Optional<Member> find(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList().stream().findAny();
    }
}
```
#### 로그 도메인
```java
@Entity
@Getter
@Setter
public class Log {

    @Id @GeneratedValue
    private Long id;
    private String message;

    public Log() {

    }

    public Log(String message) {
        this.message = message;
    }
}
```
#### 로그 레퍼지토리
```java
Slf4j
@Repository
@RequiredArgsConstructor
public class LogRepository {

    private final EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Log logMessage) {
        log.info("log 저장");
        em.persist(logMessage);

        if (logMessage.getMessage().contains("로그예외")) {
            log.info("로그 저장 시 예외 ㅏㅂㄹ생");
            throw new RuntimeException("예외 발생");
        }
    }

    public Optional<Log> find(String message) {
        return em.createQuery("select l from Log l where l.message = :message", Log.class)
                .setParameter("message", message)
                .getResultList().stream().findAny();
    }
}
```
#### 사용자 서비스 계층
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final LogRepository logRepository;
    
    public void joinV1(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username);

        log.info("== memberRepository 호출 시작 ==");
        memberRepository.save(member);
        log.info("== memberRepository 호출 종료 ==");

        log.info("== logRepository 호출 시작 ==");
        logRepository.save(logMessage);
        log.info("== logRepository 호출 종료 ==");
    }

    public void joinV2(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username);

        log.info("== memberRepository 호출 시작 ==");
        memberRepository.save(member);
        log.info("== memberRepository 호출 종료 ==");

        log.info("== logRepository 호출 시작 ==");
        try {
            logRepository.save(logMessage);
        } catch (RuntimeException e) {
            log.info("log 저장에 실패했습니다. logMessage={}", logMessage.getMessage());
            log.info("정상 흐름 반환");
        }
        log.info("== logRepository 호출 종료 ==");
    }
}
```

#### 테스트 1
```java
	/**
     * memberSErvice @Transacetional:OFF
     * memberRepository @Transctional:ON
     * logRepository @Transactional:ON
     */
	@Test
    void outerTxOff_success() {
        // given
        String username = "outerTxOff_success";

        memberService.joinV1(username);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }
```
![](https://velog.velcdn.com/images/bon0057/post/f9ca33fc-a323-432b-be29-7ffb658a28ea/image.png)


#### 테스트 2
```java
	/**
     * memberSErvice @Transacetional:OFF
     * memberRepository @Transctional:ON
     * logRepository @Transactional:ON Exception
     */
	@Test
    void outerTxOff_fail() {
        // given
        String username = "로그예외_outerTxOff_fail";

        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }
```
- 사용자 이름에 로그예외 라는 단어가 포함되어 있으면 LogRepository 에서 런타임 예외가 발생한다.
  트랜잭션 AOP는 해당 런타임 예외를 확인하고 롤백 처리한다.
  ![](https://velog.velcdn.com/images/bon0057/post/a3d5367c-6f77-4d76-b142-737dfce4dc43/image.png)

#### 테스트 3
```java
	/**
     * memberSErvice @Transacetional:ON
     * memberRepository @Transctional:OFF
     * logRepository @Transactional:OFF
     */

    @Test
    void singleTx() {
        // given
        String username = "singleTx";

        memberService.joinV1(username);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }
```
![](https://velog.velcdn.com/images/bon0057/post/4b426631-11d8-4024-bec7-2efc14d3aedf/image.png)
- @Transactional 이 MemberService 에만 붙어있기 때문에 여기에만 트랜잭션 AOP가 적용된다. MemberRepository , LogRepository 는 트랜잭션 AOP가 적용되지 않는다.
- MemberService 의 시작부터 끝까지, 관련 로직은 해당 트랜잭션이 생성한 커넥션을 사용하게 된다.

#### 테스트 4
```java
	@Test
    void outerTxOn_success() {
        // given
        String username = "outerTxOn_success";

        memberService.joinV1(username);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }
```
![](https://velog.velcdn.com/images/bon0057/post/764a952c-9257-43e0-8bdb-8069eaca36b8/image.png)

#### 테스트 5
```java
	/**
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON Exception
     */
    @Test
    void outerTxOn_fail() {
        // given
        String username = "로그예외_outerTxOn_fail";

        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        Assertions.assertTrue(memberRepository.find(username).isEmpty());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }
```
![](https://velog.velcdn.com/images/bon0057/post/8d16ccb0-5f29-4d01-b0d7-3e91c14970a6/image.png)

#### 테스트 6
- 회원 가입을 시도한 로그를 남기는데 실패하더라도 회원 가입은 유지되어야 한다.
```java
	/**
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * logRepository 	@Transactional:ON
     */
    @Test
    void recoverException_fail() {
        // given
        String username = "로그예외_recoverException_fail";

        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        Assertions.assertTrue(memberRepository.find(username).isEmpty());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }
```
![](https://velog.velcdn.com/images/bon0057/post/9f6f8baa-81fe-4305-a846-bd0953933441/image.png)

#### 테스트 7
```java
	/**
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * logRepository    @Transactional:ON (Requires_NEW)
     */
    @Test
    void recoverException_success() {
        // given
        String username = "로그예외_recoverException_success";

        memberService.joinV2(username);

        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }
```
![](https://velog.velcdn.com/images/bon0057/post/c9a40f62-efac-45ae-b022-ab8fe5cb7968/image.png)
- 논리 트랜잭션은 하나라도 롤백되면 관련된 물리 트랜잭션은 롤백되어 버린다.
- 이 문제를 해결하려면 REQUIRES_NEW 를 사용해서 트랜잭션을 분리해야 한다.



