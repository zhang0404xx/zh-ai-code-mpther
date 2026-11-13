package com.zh.zhaicodempther.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * MyBatis-Flex 代码生成器（官方模板写法，独立 main，不参与应用启动）
 * <p>
 * 在项目根目录运行 {@link #main(String[])} 即可生成 entity / mapper / service / serviceImpl / controller。
 * 数据源取自 classpath 的 application.yml；改配置后需重新构建，确保 target/classes 已同步。
 * 官方默认不覆盖同名文件，已手写的 Controller 不会被替换。
 */
public class MyBatisFlexGenerator {

    /** 应用配置（classpath 资源，由 Maven 从 src/main/resources 复制到 target/classes） */
    private static final String APPLICATION_YML = "application.yml";

    public static void main(String[] args) {
        //从 application.yml 读取 spring.datasource.*
        Dict yml = YamlUtil.loadByPath(APPLICATION_YML);

        //配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(yml.getByPath("spring.datasource.url", String.class));
        dataSource.setUsername(yml.getByPath("spring.datasource.username", String.class));
        dataSource.setPassword(yml.getByPath("spring.datasource.password", String.class));

        //创建配置内容
        GlobalConfig globalConfig = createGlobalConfig();

        //通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        //生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfig() {
        //创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        //设置根包
        globalConfig.getPackageConfig()
                .setBasePackage("com.zh.zhaicodempther.genresult");

        //设置只生成哪些表；不设置则生成所有表
        globalConfig.getStrategyConfig()
                .setGenerateTable("user", "app", "chat_history")
                //逻辑删除字段，必须与数据库列名大小写一致
                .setLogicDeleteColumn("isDelete");

        //设置生成 entity 并启用 Lombok
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);

        //设置生成 mapper
        globalConfig.enableMapper();

        //设置生成 service
        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        //设置生成 controller
        globalConfig.enableController();

        //设置生成表和实体类的注释
        globalConfig.getJavadocConfig()
                .setAuthor("zh")
                .setSince("");

        return globalConfig;
    }
}
