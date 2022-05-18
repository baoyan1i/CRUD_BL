package io.simpleframework.crud.info;

import io.simpleframework.crud.annotation.IdStrategy;
import io.simpleframework.crud.util.Snowflake;

import java.util.UUID;


@FunctionalInterface
public interface IdGenerator {

    IdGenerator UUID32_ID_GENERATOR = () -> UUID.randomUUID().toString().replace("-", "");
    IdGenerator UUID36_ID_GENERATOR = () -> UUID.randomUUID().toString();
    IdGenerator DEFAULT_SNOWFLAKE_ID_GENERATOR = new SnowflakeIdGenerator(IdStrategy.SNOWFLAKE_BEGIN_TIME);

    
    Object generate();

    class SnowflakeIdGenerator implements IdGenerator {
        private final Snowflake snowflake;

        public SnowflakeIdGenerator(long beginTime) {
            this.snowflake = new Snowflake(beginTime);
        }

        @Override
        public Object generate() {
            return snowflake.nextId();
        }

    }
}
