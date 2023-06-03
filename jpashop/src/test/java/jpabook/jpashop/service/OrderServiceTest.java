package jpabook.jpashop.service;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Item.Book;
import jpabook.jpashop.domain.Item.Item;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.exception.NotEnoughStockException;
import jpabook.jpashop.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    EntityManager em;
    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;

    @Test
    public void 상품주문() {
        //given
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울","x","x"));
        em.persist(member);

        Book book = new Book();
        book.setName("시골");
        book.setPrice(10000);
        book.setStockQuantity(10);
        em.persist(book);

        //when
        Long orderId = orderService.order(member.getId(), book.getId(), 2);

        //then
        Order getOrder = orderRepository.findOne(orderId);
        Assertions.assertEquals(OrderStatus.ORDER, getOrder.getStatus());
        Assertions.assertEquals(1, getOrder.getOrderItems().size());
        Assertions.assertEquals(10000 * 2, getOrder.getTotalPrice());
        Assertions.assertEquals(8, book.getStockQuantity());
    }

    @Test
    public void 주문취소() {
        //given
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울","x","x"));
        em.persist(member);

        Book book = new Book();
        book.setName("시골");
        book.setPrice(10000);
        book.setStockQuantity(10);
        em.persist(book);

        Long orderId = orderService.order(member.getId(), book.getId(), 2);

        //when
        orderService.cancelOrder(orderId);

        //then
        Order order = orderRepository.findOne(orderId);
        assertThat(OrderStatus.CANCLE).isEqualTo(order.getStatus());
        assertThat(book.getStockQuantity()).isEqualTo(10);
    }

    @Test
    public void 상품주문_재고수량초과() {
        //given
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울","x","x"));
        em.persist(member);

        Book book = new Book();
        book.setName("시골");
        book.setPrice(10000);
        book.setStockQuantity(10);
        em.persist(book);

        //when
        assertThatThrownBy(() -> orderService.order(member.getId(), book.getId(), 11))
                .isInstanceOf(NotEnoughStockException.class);

        //then
        //fail("재고 수량 부족 예외가 발생해야 함 ");
    }
}