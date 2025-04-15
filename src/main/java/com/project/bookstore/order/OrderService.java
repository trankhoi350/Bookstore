package com.project.bookstore.order;

import com.project.bookstore.book.Book;
import com.project.bookstore.cart.Cart;
import com.project.bookstore.cart.CartRepository;
import com.project.bookstore.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, CartRepository cartRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
    }

    public Order getUserOrder(String email){
        Cart cart = cartRepository.findByUser(new User(email)).orElseThrow(() -> new RuntimeException("Cart not found"));
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Status.WAIT);

        List<OrderItem> items = cart.getItems().stream().map(item -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setBook(item.getBook());
            orderItem.setQuantity(item.getQuantity());
            return orderItem;
        }).collect(Collectors.toList());

        order.setItems(items);
        Order saveOrder = orderRepository.save(order);

        cartRepository.delete(cart);
        return saveOrder;
    }

}
