package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional //test에 이게 있으면 기본적으로 rollback을 해버림
@Commit
class QuerydslApplicationTests {

	@Autowired
	//@PersistenceContext //나 이거 첨봐..!! springframework 공식..?
	EntityManager em;


	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory query = new JPAQueryFactory(em);
		//QHello qHello = new QHello("h");
		QHello qHello = QHello.hello; // 이렇게 써두됨 이미 선언 되어있음

		Hello result = query.selectFrom(qHello)
				.fetchOne();

		//Assertions.assertThat() //옵션+엔터로 Assertion을 static으로 바꿈
		assertThat(result).isEqualTo(hello);
		assertThat(result.getId()).isEqualTo(hello.getId()); //lombok test
	}

}
