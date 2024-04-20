package org.aurora.controller.user;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.aurora.entity.AddressBook;
import org.aurora.result.Result;
import org.springframework.web.bind.annotation.*;


/**
 * 地址簿(AddressBook)表控制层
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
@RestController
@RequestMapping("addressBook")
@Slf4j
@Api(tags = "C端-地址簿接口")
public class AddressBookController {

  @PostMapping("")

    public Result<String> add(@RequestBody AddressBook addressBook) {
        log.info("新增地址:{}", addressBook);
        return null;
    }
}

