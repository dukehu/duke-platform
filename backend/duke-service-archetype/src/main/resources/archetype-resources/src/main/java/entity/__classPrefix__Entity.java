package ${package}.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ${classPrefix} 瀹炰綋
 * 褰?useDatabase=n 鏃跺彲鍒犻櫎姝ゆ枃浠跺強 mapper 鍖? */
#if($useDatabase != "y")
// 娉ㄦ剰锛歶seDatabase=n锛屾鏂囦欢浠呬綔鍗犱綅锛屽彲瀹夊叏鍒犻櫎
// 鍚屾椂鍒犻櫎 mapper/${classPrefix}Mapper.java 鍜?resources/mapper/${classPrefix}Mapper.xml
#end
@Data
@TableName("${classPrefixLower}")
public class ${classPrefix}Entity {

    @TableId(type = IdType.AUTO)
    private Long id;

    // TODO

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}



