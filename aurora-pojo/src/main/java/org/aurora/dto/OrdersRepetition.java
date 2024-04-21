package org.aurora.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrdersRepetition {

    private Integer number;   //数量
    private ShoppingCartDTO shoppingCartDTO;
}