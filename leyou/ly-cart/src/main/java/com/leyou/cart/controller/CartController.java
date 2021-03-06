package com.leyou.cart.controller;

import com.leyou.cart.pojo.Cart;
import com.leyou.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 添加购物车
     *
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> addCart(@RequestBody Cart cart) {
        this.cartService.addCart(cart);
        return ResponseEntity.ok().build();
    }

//    /**
//     * 查询购物车列表
//     *
//     * @return
//     */
//    @GetMapping
//    public ResponseEntity<List<Cart>> queryCartList() {
//        List<Cart> carts = this.cartService.queryCartList();
//        if (carts == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//        return ResponseEntity.ok(carts);
//    }

    /**
     *
     * @param newCarts newCarts 之前存在 Web localStorage的购物车，需要于redis的购物车合并
     * @return
     */
    @PostMapping("getCards")
    public ResponseEntity<List<Cart>> queryCartList(@RequestBody List<Cart> newCarts) {
        List<Cart> carts = this.cartService.queryCartList(newCarts);
        if (carts == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(carts);
    }


    /**
     *
     * @param skuId
     * @param num
     * @return
     */
    @PutMapping
    public ResponseEntity<Void> updateNum(@RequestParam("skuId") Long skuId,
                                          @RequestParam("num") Integer num) {
        this.cartService.updateNum(skuId, num);
        return ResponseEntity.ok().build();
    }
}