package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository   //dao와 비슷한 개념 //data를 접근하는 계층
public class MemberJpaRepository { //순수 jpa 레파지토리

    private final EntityManager em; //순수 jpa는 접근할 때 em이 필요하다.
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em,JPAQueryFactory jpaQueryFactory) {
        this.em = em;
       // this.queryFactory = new JPAQueryFactory(em); //스프링 빈으로 등록 안했을 때
        //Q. 동시성 문제가 발생하지 않나요?  -> em,jpafactory 접근에 대한 동시성 문제는 em에 다 의존하는데
        //em은 트랜잭션 단위로 따로따로 분리되서 동작함. em은 스프링이 프록시인 가짜를 주입, 걔가 트랜잭션 단위로 다 다른데 바인딩 되도록 라우팅만해줌.
        //어떻게 쓰든.
        this.queryFactory = jpaQueryFactory;
        //스프링 빈으로 등록 했을 때 장점 : @RequiredArgsConstructor, 단점: injection을 두번이나 해줘야한다.
    }

    public void save(Member member) {
        em.persist(member);
    }
    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member).fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username " +
                "= :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    } //오타가 나도 실행이 됨


    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username)) //파라미터 바인딩
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch(); //한번에.. 성능최적화가 된다..왜..?
    }

    public List<Member> searchMember(MemberSearchCondition condition) {
        return queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeLoe(),condition.getAgeGoe()))
                .fetch();
    }


    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch(); //가독성, 재사용성 높다
    }

    private BooleanExpression ageBetween(int ageLoe,int ageGoe) { //조립 가능
        return ageLoe(ageLoe).and(ageGoe(ageGoe));
    }


    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression teamNameEq(String teamName) { //메서드 위로 올리는거 cmd+shift+방향키
        return hasText(teamName)? team.name.eq(teamName) : null;
    }

    //Predicate 보다는 BooleanExpression 이 더 났다. 나중에 조합하기 쉽다.
    private BooleanExpression usernameEq(String username) {
        return hasText(username)? member.username.eq(username) : null;
    }

}
