package ${package}.service;

import ${package}.dto.${classPrefix}QueryDTO;
import com.duke.framework.common.PageResult;

public interface I${classPrefix}Service {

    PageResult<Object> page(${classPrefix}QueryDTO dto);

    Object getById(Long id);

    void delete(Long id);
}



