package com.tim07.controller;

import com.tim07.domain.Autentification.JwtUser;
import com.tim07.domain.DTO.OrderItemDTO;
import com.tim07.domain.DTO.TableOrderDTO;
import com.tim07.domain.Entity.*;
import com.tim07.domain.Enumeration.ItemType;
import com.tim07.domain.Enumeration.OrderItemState;
import com.tim07.service.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brzak on 17.6.17..
 */
@RestController
@RequestMapping("/tableOrder")
public class TableOrderController {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private TableOrderService tableOrderService;

    @Autowired
    private WaiterService waiterService;

    @Autowired
    private ChefService chefService;

    @Autowired
    private BarmanService barmanService;

    @Autowired
    private JwtService jwtService;

    @RequestMapping(
            value = "create",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TableOrderDTO> createOrder(@RequestHeader("Authorization") String userToken,
                                      @RequestBody TableOrderDTO tableOrderDTO)
    {

        JwtUser user = this.jwtService.getUser(userToken);

        Waiter waiter = this.waiterService.getByUsername(user.getUsername());

        if (waiter != null){

            Restaurant restaurant = waiter.getRestaurant();

            if(tableOrderDTO.getOrderItems().isEmpty())
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

            TableOrder tableOrder = tableOrderService.createOrder(tableOrderDTO.getOrderItems(),restaurant, tableOrderDTO.getTableId());

            if(tableOrder == null)
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);


            return new ResponseEntity<>(convertTableOrderToDTO(tableOrder),HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(
            value = "finish",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TableOrderDTO> finishOrder(@RequestHeader("Authorization") String userToken,
                                                     @RequestBody TableOrderDTO tableOrderDTO)
    {

        JwtUser user = this.jwtService.getUser(userToken);

        Waiter waiter = this.waiterService.getByUsername(user.getUsername());

        if (waiter != null && tableOrderDTO.isReady()){

            Restaurant restaurant = waiter.getRestaurant();

            TableOrder tableOrder = tableOrderService.finishOrder(tableOrderDTO.getId());


            return new ResponseEntity<>(convertTableOrderToDTO(tableOrder),HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(
            value = "editItem",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OrderItemDTO> editItem(@RequestHeader("Authorization") String userToken,
                                                     @RequestBody OrderItemDTO orderItemDTO)
    {

        JwtUser user = this.jwtService.getUser(userToken);

        Waiter waiter = this.waiterService.getByUsername(user.getUsername());

        if (waiter != null){

            Restaurant restaurant = waiter.getRestaurant();

            OrderItem orderItem = null;

            try
            {
                orderItem  = this.tableOrderService.setItemAmount(orderItemDTO.getId(),orderItemDTO.getAmount());

            }catch (OptimisticLockingFailureException e) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }


            if(orderItem == null)
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

            return new ResponseEntity<>(converOrderItemToDTO(orderItem),HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(
            value = "removeItem",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OrderItemDTO> removeItem(@RequestHeader("Authorization") String userToken,
                                                 @RequestBody OrderItemDTO orderItemDTO)
    {

        JwtUser user = this.jwtService.getUser(userToken);

        Waiter waiter = this.waiterService.getByUsername(user.getUsername());

        if (waiter != null){

            Restaurant restaurant = waiter.getRestaurant();

            try{
                if(this.tableOrderService.removeItem(orderItemDTO.getId()))
                    return new ResponseEntity<OrderItemDTO>(orderItemDTO,HttpStatus.OK);
                else
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

            }catch (OptimisticLockingFailureException e)
            {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }



        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(
            value = "getWaiterOrders",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<TableOrderDTO>> getWaiterOrders(@RequestHeader("Authorization") String userToken)
    {

        JwtUser user = this.jwtService.getUser(userToken);

        Waiter waiter = this.waiterService.getByUsername(user.getUsername());

        if (waiter != null){

            Restaurant restaurant = waiter.getRestaurant();
            List<TableOrderDTO> tableOrders = new ArrayList<>();

            for(TableOrder order : restaurant.getTableOrders()){
                for(TableSegment segment : waiter.getTableSegments()){
                    if(segment.getSegment() == order.getRestaurantTable().getSegment() && !order.isFinished()){
                        tableOrders.add(convertTableOrderToDTO(order));
                    }
                }
            }

            return new ResponseEntity<>(tableOrders,HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(
            value = "getChefDishes",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<OrderItemDTO>> getChefDishes(@RequestHeader("Authorization") String userToken)
    {

        JwtUser user = this.jwtService.getUser(userToken);

        Chef chef = this.chefService.findByUsername(user.getUsername());

        if (chef != null){

            Restaurant restaurant = chef.getRestaurant();
            List<OrderItemDTO> orderItemDTOS = new ArrayList<>();

            for(TableOrder order : restaurant.getTableOrders()){
                for(OrderItem item : order.getOrderItems()){
                    if(item.getType() == ItemType.Dish && item.getState() == OrderItemState.Waiting)
                        orderItemDTOS.add(converOrderItemToDTO(item));
                }
            }

            return new ResponseEntity<>(orderItemDTOS,HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }


    @RequestMapping(
            value = "getBarmanDrinks",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<OrderItemDTO>> getBarmanDrinks(@RequestHeader("Authorization") String userToken)
    {

        JwtUser user = this.jwtService.getUser(userToken);

        Barman barman = this.barmanService.findByUsername(user.getUsername());

        if (barman != null){

            Restaurant restaurant = barman.getRestaurant();
            List<OrderItemDTO> orderItemDTOS = new ArrayList<>();

            for(TableOrder order : restaurant.getTableOrders()){
                for(OrderItem item : order.getOrderItems()){
                    if(item.getType() == ItemType.Drink && item.getState() == OrderItemState.Waiting)
                        orderItemDTOS.add(converOrderItemToDTO(item));
                }
            }

            return new ResponseEntity<>(orderItemDTOS,HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(
            value = "getPreparingDishes",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<OrderItemDTO>> getPreparingDishes(@RequestHeader("Authorization") String userToken)
    {

        JwtUser user = this.jwtService.getUser(userToken);

        Chef chef = this.chefService.findByUsername(user.getUsername());

        if (chef != null){

            Restaurant restaurant = chef.getRestaurant();
            List<OrderItemDTO> orderItemDTOS = new ArrayList<>();

            for(TableOrder order : restaurant.getTableOrders()){
                for(OrderItem item : order.getOrderItems()){
                    if(item.getType() == ItemType.Dish && item.getState() == OrderItemState.Preparing)
                        orderItemDTOS.add(converOrderItemToDTO(item));
                }
            }

            return new ResponseEntity<>(orderItemDTOS,HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(
            value = "setOrderState",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OrderItemDTO> getChefDishes(@RequestHeader("Authorization") String userToken, @RequestBody OrderItemDTO item)
    {

        JwtUser user = this.jwtService.getUser(userToken);

        Chef chef = this.chefService.findByUsername(user.getUsername());
        Barman barman = this.barmanService.findByUsername(user.getUsername());

        if (chef != null || barman != null){

            try
            {
                return new ResponseEntity<>(converOrderItemToDTO(tableOrderService.setItemState(item.getId(),item.getState())),HttpStatus.OK);

            }catch (OptimisticLockingFailureException e)
            {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }

        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }



    private TableOrderDTO convertTableOrderToDTO(TableOrder order){
        ModelMapper mapper = new ModelMapper();
        return mapper.map(order, TableOrderDTO.class);
    }

    private OrderItemDTO converOrderItemToDTO(OrderItem item)
    {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(item, OrderItemDTO.class);
    }

}
