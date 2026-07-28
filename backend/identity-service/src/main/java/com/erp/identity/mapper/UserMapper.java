package com.erp.identity.mapper;

import com.erp.identity.dto.RegistrationRequest;
import com.erp.common.security.Role;
import com.erp.identity.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "role", source = "role", qualifiedByName = "mapRole")
    User toEntity(RegistrationRequest dto);

    @Named("mapRole")
    default Role mapRole(String roleStr) {
        return Role.valueOf(roleStr.toUpperCase());
    }
}