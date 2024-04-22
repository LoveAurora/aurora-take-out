package org.aurora.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.context.BaseContext;
import org.aurora.entity.AddressBook;
import org.aurora.result.Result;
import org.aurora.service.AddressBookService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 地址簿(AddressBook)表控制层
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
@RestController
@Slf4j
@RequestMapping("/user/addressBook")
@Api(tags = "C端-地址簿接口")
public class AddressBookController {

    private final AddressBookService addressBookService;

    public AddressBookController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }


    /**
     * 新增地址
     *
     * @param addressBook 地址簿实体，包含地址的详细信息
     */
    @PostMapping("")
    @ApiOperation(value = "新增地址")
    public Result<String> add(@RequestBody AddressBook addressBook) {
        log.info("新增地址:{}", addressBook);
        Long id = BaseContext.getCurrentId();
        addressBook.setUserId(id);
        addressBook.setIsDefault(0);
        addressBookService.save(addressBook);
        return Result.success();
    }

    /**
     * 查询当前登录用户的所有地址信息
     */
    @GetMapping("/list")
    @ApiOperation("查询当前登录用户的所有地址信息")
    public Result<List<AddressBook>> list() {
        log.info("查询当前登录用户的所有地址信息:");
        return Result.success(addressBookService
                .list(new LambdaQueryWrapper<AddressBook>()
                        .eq(AddressBook::getUserId, BaseContext.getCurrentId())));
    }

    /**
     * 修改地址
     *
     * @param addressBook 地址簿实体，包含地址的详细信息
     */
    @PutMapping
    @ApiOperation("根据id修改地址")
    public Result<String> update(@RequestBody AddressBook addressBook) {
        log.info("修改地址:{}", addressBook);
        addressBookService.updateById(addressBook);
        return Result.success();
    }

    /**
     * 根据id查询地址
     *
     * @param id 地址id
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询地址")
    public Result<AddressBook> get(@PathVariable Long id) {
        log.info("根据id查询地址:{}", id);
        AddressBook addressBook = addressBookService.getById(id);
        return Result.success(addressBook);
    }

    /**
     * 根据id删除地址
     *
     * @param id 地址id
     */
    @DeleteMapping("/{id}")
    @ApiOperation("根据id删除地址")
    public Result<String> delete(@PathVariable Long id) {
        log.info("根据id删除地址:{}", id);
        addressBookService.removeById(id);
        return Result.success("删除成功");
    }


    /**
     * 查询默认地址
     */

    @GetMapping("/default")
    @ApiOperation("查询默认地址")
    public Result<AddressBook> getDefault() {
        log.info("查询默认地址");
        AddressBook addressBook = addressBookService
                .getOne(new LambdaQueryWrapper<AddressBook>()
                        .eq(AddressBook::getUserId, BaseContext.getCurrentId())
                        .eq(AddressBook::getIsDefault, 1));
        return Result.success(addressBook);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook 地址簿实体，包含地址的详细信息
     */
    @PutMapping("/default")
    @ApiOperation("设置默认地址")
    @Transactional
    public Result<String> setDefault(@RequestBody AddressBook addressBook) {
        log.info("设置默认地址:");
        // 1. 将当前用户所有的地址都设置为非默认地址
        addressBookService.update(new LambdaUpdateWrapper<AddressBook>()
                .eq(AddressBook::getUserId, BaseContext.getCurrentId())
                .set(AddressBook::getIsDefault, 0));
        // 2. 将当前地址设置为默认地址
        addressBookService.update(new LambdaUpdateWrapper<AddressBook>()
                .eq(AddressBook::getId, addressBook.getId())
                .set(AddressBook::getIsDefault, 1));
        return Result.success("设置默认地址成功");

    }


}

