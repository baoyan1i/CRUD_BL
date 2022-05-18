Target of the code:
	Base CRUD, The basic principle of the code is to register
	data model classes to Models, domain model classes to Repos
	and map them to table and field information in the database 
	according to the class definition

Install:
	Maven:
	<dependency>
    		<groupId>io.simpleframework</groupId>
    		<artifactId>crud</artifactId>
    		<version>1.1.1</version>
	</dependency>
	Gradle:
		implementation 'io.simpleframework:crud:1.1.1'

Apply:
	Single Table:
		CREATE TABLE user
		(
    			id   VARCHAR(50) NOT NULL,
    			name VARCHAR(50) COMMENT '姓名',
    			PRIMARY KEY (`id`)
		);
	Easy applied:
		import io.simpleframework.crud.Models;

		// 定义数据模型类
		public class User {
    			String id;
    			String name;
   		 
		}


		public class Demo {

    			public static void test() {
        			User user = new User();
       			 user.setName("test");
        			Models.mapper(User.class).insert(user);
    			}

		}
		
Easier(By using Active Record MOD):
		import io.simpleframework.crud.BaseModel;
	
	
	public class User extends BaseModel<User> {
	    String id;
	    String name;
	    
	}
	
	// 使用
	public class Demo {
	
	    public static void test() {
	        User user = new User();
	        user.setName("测试");
	        user.insert();
	    }
	
	}