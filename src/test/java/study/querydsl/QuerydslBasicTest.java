package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.awt.print.Pageable;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach // 개별 테스트 실행되기 전에 실행됨
    public void before() {

        //em를 통해서 쿼리를 찾음 //쿼리틀리면 컴파일 시점에 알려줌 //before에 선언해도됨
        //em이 멀티스레드 환경에 잘되도록 설계되어있음 트랜잭션에 따라 잘 분배해줌.
        //멀티스레드 환경에서 동시성 문제 없이 사용할 수 있도록 선언되어있다.
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라. //쿼리 틀려도 런타임에야 알 수 있음
        String qlString = "select m from Member m where m.username =: username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        //변수 : 별칭, 어떤 Q멤버인지를 구분하는 이름 : 크게 안중요함
        // QMember m = new QMember("m"); //어떤 Q멤버인지를 구분하는 이름
        // QMember m = QMember.member; //이미 static final로 선언되어있음
        // import static study.querydsl.entity.QMember.member; 사용

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))//파라미터 바인딩이 자동 //코드 어시스선트가 어마어마함
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10, 30)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void searchAndParam() {

        Member findMember = queryFactory
                .selectFrom(member)
                //and는 , 로 사용 가능  , querydsl은 and null은 무시함
                .where(member.username.eq("member1"),
                        (member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //단 건
        Member findMember1 = queryFactory
                .selectFrom(member)
                .fetchOne();

        //처음 한 건 조회
        Member findMember2 = queryFactory
                .selectFrom(member)
                //   .limit(1).fetchOne();
                .fetchFirst();

        //페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        //total count 쿼리랑 contents 쿼리 두번 실행

        results.getTotal();
        List<Member> members = results.getResults();

        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 1. 회원 나이 내림 차순(desc)
     * 2. 회원 이름 올림 차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //몇까지를 스킵할거야 //sql방언에 따라 조회
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //몇까지를 스킵할거야 //sql방언에 따라 조회
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);

    }

    @Test
    public void aggregation() {
        //Tuple : querydsl tuple : 여러개 타입이 있을 때 막 꺼내올 수 있는 것. 데이터 타입 여러개를 조회할 때 사용, 실무에서는 dto로 뽑아오는방법을 사용
        List<Tuple> result = queryFactory.select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min()
        ).from(member).fetch(); //fetchone도 가능

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
        /* select
        count(member1),
        sum(member1.age),
        avg(member1.age),
        max(member1.age),
        min(member1.age)
    from
        Member member1 */
    }

    /**
     * 팀으 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void groupBy() {
        List<Tuple> result = queryFactory.select(team.name, member.age.avg()).from(member)
                .join(member.team, team)
                .groupBy(team.name).fetch(); //having도 가능. groupby한 결과중에서 조건을 거는 것

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
        /* select
        team.name,
        avg(member1.age)
    from
        Member member1
    inner join
        member1.team as team
    group by
        team.name */
    }

    //조인 : 첫번째 파라미터에 조인 대상, 두번째 파라미터 : 별칭으로 사용할 Q타입 지정

    /**
     * 팀 A에 소속한 모든 회원
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username")
                .containsExactly("member1", "member2");

    }

         /* 그냥 조인. select
        member1
    from
        Member member1
    inner join
        member1.team as team
    where
        team.name = ?1 */


        /* left out join :  select
        member1
    from
        Member member1
    left join
        member1.team as team
    where
        team.name = ?1 */

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() { //연관관계없이 조회 : 과거에는 outer join은 불가, 현재는 join on을 사용해서 외부 join 할 수 있음
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // 그냥 나열
                .where(member.username.eq(team.name)) //전부 가져온 다음에 조건절에서 거름 DB마다 성능최적화하는 방법이 다르지만 성능최적화함
                .fetch();

        assertThat(result).extracting("username")
                .containsExactly("teamA", "teamB");

            /* select
        member1
    from
        Member member1,
        Team team
    where
        member1.username = team.name */
            /*
             select
            member0_.member_id as member_i1_1_,
            member0_.age as age2_1_,
            member0_.team_id as team_id4_1_,
            member0_.username as username3_1_
        from
            member member0_ cross
        join
            team team1_
        where
            member0_.username=team1_.name
             */
    }

    /**
     * 조인 1. 조인대상 필터링
     * 조인 2. 연관관계 없는 엔티티 조인
     * 예) 회원 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * jpql: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() { //정말 외부조인이 필요한 경우에만 사용
        List<Tuple> result = queryFactory.select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch(); //멤버는 다 나오지만 팀은 팀이 a인 것만

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        /*
        tuple = [Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
        tuple = [Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
        tuple = [Member(id=5, username=member3, age=30), null]
        tuple = [Member(id=6, username=member4, age=40), null]
         */

        /*
         select member1, team
            from Member member1
             left join member1.team as team with team.name = ?1
          select member0_.member_id as member_i1_1_0_, team1_.id as id1_2_1_, member0_.age as age2_1_0_, member0_.team_id as team_id4_1_0_, member0_.username as username3_1_0_, team1_.name as name2_2_1_ from member member0_ left outer join team team1_ on member0_.team_id=team1_.id and (team1_.name=?)

         */
    }

    /**
     * 연간관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() { //연관관계없이 조회 : 과거에는 outer join은 불가, 현재는 join on을 사용해서 외부 join 할 수 있음
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member,team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        /*/* select member1, team
from Member member1
  left join Team team with member1.username = team.name /
  select member0_.member_id as member_i1_1_0_, team1_.id as id1_2_1_, member0_.age as age2_1_0_, member0_.team_id as team_id4_1_0_, member0_.username as username3_1_0_, team1_.name as name2_2_1_ from member member0_ left outer join team team1_ on (member0_.username=team1_.name)
          */
    }

    //페치조인
    //:연관된 엔티티를 한번에 가져오는 방법 : 성능최적화
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")).fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치조인 미적용").isFalse();


        /* select member1
        from Member member1
        where member1.username = ?1 */
    }

    @Test //실무에서 진짜 많이쓴다.
    public void fetchJoinUSe(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1")).fetchOne();

         /* select
        member1
    from
        Member member1
    inner join
        fetch member1.team as team
    where
        member1.username = ?1 */
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치조인 미적용").isFalse();
    }



    @Test
    public void sampleProjection(){
        List<String> result = queryFactory //타입을 String으로 지정 가능
                .select(member.username)
                .from(member).fetch();

        for(String s:result){
            System.out.println("s = "+s);
        }

         /* select
        member1.username
    from
        Member member1 */

        /*
        s = member1
        s = member2
        s = member3
        s = member4
         */
    }

    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory. //반환 타입이 Tuple로 생성됨
                select(member.username, member.age)
                .from(member)
                .fetch();

        for(Tuple tuple:result){
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("s = "+username +", age = "+age);
        }
          /* select
        member1.username,
        member1.age
        from
        Member member1 */

        /*s = member1, age = 10
        s = member2, age = 20
        s = member3, age = 30
        s = member4, age = 40*/
    }

    @Test
    public void findDtoByJPQL(){
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for(MemberDto memberDto:result){
            System.out.println("MemberDto = "+memberDto);
        }

         /* select
        new study.querydsl.dto.MemberDto(m.username,
        m.age)
    from
        Member m */
    }

    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, //setter활용
                        member.username,
                        member.age)) //dto를 만들고 걊을 셋팅해주기때문에 기본생성자 필요
                .from(member)
                .fetch();

        for(MemberDto memberDto:result){
            System.out.println("MemberDto = "+memberDto);
        }

          /* select
        member1.username,
        member1.age
    from
        Member member1 */
    }

    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, //getter,setter필요없음. 바로 필드에 값을 꽂음
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("MemberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("MemberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub), "age")
                        )
                ).from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }

        /* select
        member1.username as name,
        (select
            max(memberSub.age)
        from
            Member memberSub) as age
    from
        Member member1 */
    }

    @Test //가장 빠른 해결책인데, 단점도 많다. (근데 뭐에 대한 해결책...?)
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory //타입을 그대로 가져가기 때문에, 타입이 틀리면 에러를 준다.
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();


        for (MemberDto memberDto : result) {
            System.out.println("MemberDto = " + memberDto);
        }

        /*
        Constructor와의 차이
        : Constructor는 컴파일 시점에 에러를 못 잡고, 런타임 시점에 오류가 난다.
         */
    }


    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;
        List result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }


    private List searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();

         /* select
        member1
    from
        Member member1
    where
        member1.username = ?1
        and member1.age = ?2 */
    }


    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;
        List result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond),
                        ageEq(ageCond)) //where 절은 null을 무시한다.
                .fetch();

         /* select
        member1
    from
        Member member1
    where
        member1.username = ?1
        and member1.age = ?2 */
    }
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    @Test
    public void bulkUpdate(){
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute(); //영향을 받은 row수가 반환

        System.out.println(count);

        em.flush();
        em.clear();
        /* update Member member1
            set member1.username = ?1
            where member1.age < ?2 */

        List<Member> result = queryFactory.selectFrom(member).fetch();
        for(Member member:result){ //리프트볼리드..?
            System.out.println("member = " + member);
        }
        /*
        member = Member(id=3, username=member1, age=10)
        member = Member(id=4, username=member2, age=20)
        member = Member(id=5, username=member3, age=30)
        member = Member(id=6, username=member4, age=40)
         */
    }


    @Test
    public void bulkAdd(){
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.subtract(1)) //빼기는 : add(-1)
                .execute();

          /* update
                Member member1
            set
                member1.age = member1.age + ?1 */

    }

    @Test
    public void bulkDelete(){
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        /* delete from Member member1
            where member1.age > ?1 */
    }

    @Test
    public void sqlFunction(){
        List<String> result = queryFactory
                .select
                        (Expressions.stringTemplate("function('replace', {0}, {1},{2})",
                                member.username, "member", "M")) //member란 단어를 M으로 바꿔 조회
                .from(member)
                .fetch();

        /* select
        function('replace',
        member1.username,
        ?1,
        ?2)
        from
        Member member1 */

        /* select
            replace(member0_.username,
            ?,
            ?) as col_0_0_
        from
            member member0_ limit ? */

        for(String s : result){
            System.out.println("s = "+ s);
        }

        /*  s = M1
            s = M2
            s = M3
            s = M4 */
    }

    @Test
    public void sqlFuction2(){
        List<String> result = queryFactory.select(member.username)
                .from(member)
                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})",
                        member.username))).fetch();

        List<String> result2 = queryFactory.select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower())).fetch();
        //위 아래 같은 것. querydsl이 상당 부분 내장하고 있으니 참고해서 사용할 것

        /* select member1.username
            from Member member1
            where member1.username = function('lower', member1.username) */

        for(String s : result){
            System.out.println("s = "+ s);
        }

    }
}
