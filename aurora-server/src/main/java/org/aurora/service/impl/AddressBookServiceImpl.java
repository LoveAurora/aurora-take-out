package org.aurora.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.entity.AddressBook;
import org.aurora.mapper.AddressBookMapper;
import org.aurora.service.AddressBookService;
import org.springframework.stereotype.Service;

/**
 * 地址簿(AddressBook)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
@Service("addressBookService")
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {


}

