package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entitiy.Member;
import study.querydsl.entitiy.QMember;
import study.querydsl.entitiy.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entitiy.QMember.*;
import static study.querydsl.entitiy.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @PersistenceContext
    EntityManager em;
    JPAQueryFactory queryFactory;

    /**현재 클래스의 각
     @Test,
     @RepeatedTest,
     @ParameterizedTest,
     @TestFactory
     메소드보다 먼저 실행되어야함을 의미
     **/
    @BeforeEach
    void before() {
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
    void startJPQL() {
        //member1 을 찾아라
        String qlString = "select m from Member m where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() {
        //querydsl은 JPQL의 빌더
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member2"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member2");
    }

    @Test
    void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        (member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() {
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .fetchFirst();
//
//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬
     * 1. 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() {

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));


        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("member5");
        assertThat(result.get(1).getUsername()).isEqualTo("member6");
        assertThat(result.get(2).getUsername()).isNull();
    }

    @Test
    void paging1() {
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(results.getResults().size()).isEqualTo(2);
        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getLimit()).isEqualTo(2);
    }

    @Test
    void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.min(),
                        member.age.max()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     * @throws Exception
     */
    @Test
    void group() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) //join(조인 대상, 별칭으로 사용할 Q타입)
                .groupBy(team.name)
                .fetch();
        //when
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        //then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
    }

    /**
     * 팀 A에 소속된 모든 회원
     * @throws Exception
     */
    @Test
    void join() throws Exception {
        //given
        /**
         * select
         *         member1
         *     from
         *         Member member1
         *     inner join
         *         member1.team as team
         *     where
         *         team.name = ?1
         */
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //join(조인 대상, 별칭으로 사용할 Q타입)
                .where(team.name.eq("teamA"))
                .fetch();
        /**
         * select
         *             member0_.id as id1_1_,
         *             member0_.age as age2_1_,
         *             member0_.team_id as team_id4_1_,
         *             member0_.username as username3_1_
         *         from
         *             member member0_
         *         inner join
         *             team team1_
         *                 on member0_.team_id=team1_.id
         *         where
         *             team1_.name=?
         */
        //when

        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인(연관관계가 없어도 조인)
     * 회원의 이름이 팀 이름과 같은 회원을 조회
     * @throws Exception
     */
    @Test
    void thetaJoin() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        /**
         * select
         *         member1
         *     from
         *         Member member1,
         *         Team team
         *     where
         *         member1.username = team.name
         */

        /**
         * 모든 회원, 팀을 갖고 온 다음에 다 조인하고 where절에서 필터링
         * 보통 DB가 성능 최적화를 한다.
         */
        //when

        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * ON절을 활용한 조인
     * 1. 조인 대상 필터링
     * 2. 연관관계 없는 엔티티 외부 조인
     *
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * @throws Exception
     */
    @Test
    void joinOn() throws Exception {
        //given
        List<Tuple> result1 = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA")) //왼쪽은 무조건 표시하고, 매치되는 레코드가 오른쪽에 없으면 NULL을 표시한다.
                .fetch();

        for (Tuple tuple : result1) {
            System.out.println("tuple1 = " + tuple);
            /**
             * tuple = [Member(id=1, username=member1, age=10), Team(name=teamA)]
             * tuple = [Member(id=2, username=member2, age=20), Team(name=teamA)]
             * tuple = [Member(id=3, username=member3, age=30), null]
             * tuple = [Member(id=4, username=member4, age=40), null]
             */
        }

        /**
         * on 절을 활용해 조인 대상을 필터링 할 때,
         * 내부조인(inner join)을 사용하면,
         * where 절에서 필터링 하는 것과 기능이 동일하다.
         *
         * 따라서, on 절을 활용한 조인 대상 필터링을 사용할 때,
         * 내부조인 이면 익순한 where 절로 해결하고, 정말로 외부조인이 필요한 경우에만 이 기능(on 절)을 사용..!
         */
        List<Tuple> result2 = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
                //.on(team.name.eq("teamA"))
                .where(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result2) {
            System.out.println("tuple2 = " + tuple);
        }
        //when

        //then
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름과 팀의 이름이 같은 대상 외부조인
     * @throws Exception
     */
    @Test
    void joinOnNoRelation() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) //on 조건으로만 필터
                //.leftJoin(member.team, team) //여기서 member.team_id = team_id로 조인이 된다.
                .on(member.username.eq(team.name))
                .fetch();
        /**
         * select
         *         member1,
         *         team
         *     from
         *         Member member1
         *     left join
         *         Team team with member1.username = team.name
         *
         *
         * select
         *             member0_.id as id1_1_0_,
         *             team1_.id as id1_2_1_,
         *             member0_.age as age2_1_0_,
         *             member0_.team_id as team_id4_1_0_,
         *             member0_.username as username3_1_0_,
         *             team1_.name as name2_2_1_
         *         from
         *             member member0_
         *         left outer join
         *             team team1_
         *                 on (
         *                     member0_.username=team1_.name
         *                 )
         */

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
            /**
             * tuple = [Member(id=1, username=member1, age=10), null]
             * tuple = [Member(id=2, username=member2, age=20), null]
             * tuple = [Member(id=3, username=member3, age=30), null]
             * tuple = [Member(id=4, username=member4, age=40), null]
             * tuple = [Member(id=5, username=teamA, age=0), Team(name=teamA)]
             * tuple = [Member(id=6, username=teamB, age=0), Team(name=teamB)]
             */
        }
        //when

        //then
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNotUse() throws Exception {
        //given
        /**
         * 패치조인을 테스트할 때
         * 영속성 컨텍스트에 남아있는 객체를 안지워주면
         * 결과를 제대로 보기가 어렵다고 한다.
         */
        em.flush();
        em.clear();
        //when

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
        //then
    }

    @Test
    void fetchJoinUse() throws Exception {
        //given
        /**
         * 패치조인을 테스트할 때
         * 영속성 컨텍스트에 남아있는 객체를 안지워주면
         * 결과를 제대로 보기가 어렵다고 한다.
         */
        em.flush();
        em.clear();
        //when

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        /**
         * select
         *         member1
         *     from
         *         Member member1
         *     inner join
         *         fetch member1.team as team
         *     where
         *         member1.username = ?1
         */

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 적용").isTrue();
        //then
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery() throws Exception {

        QMember memberSub = new QMember("memberSub");

        //given
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        //when
        /**
         * select
         *             member0_.id as id1_1_,
         *             member0_.age as age2_1_,
         *             member0_.team_id as team_id4_1_,
         *             member0_.username as username3_1_
         *         from
         *             member member0_
         *         where
         *             member0_.age=(
         *                 select
         *                     max(member1_.age)
         *                 from
         *                     member member1_
         *             )
         */
        
        //then
        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    void subQueryGoe() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        /**
         * select
         *             member0_.id as id1_1_,
         *             member0_.age as age2_1_,
         *             member0_.team_id as team_id4_1_,
         *             member0_.username as username3_1_
         *         from
         *             member member0_
         *         where
         *             member0_.age>=(
         *                 select
         *                     avg(cast(member1_.age as double))
         *                 from
         *                     member member1_
         *             )
         */
        //when

        //then
        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    @Test
    void subQueryIn() throws Exception {

        //given
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        /**
         * select
         *             member0_.id as id1_1_,
         *             member0_.age as age2_1_,
         *             member0_.team_id as team_id4_1_,
         *             member0_.username as username3_1_
         *         from
         *             member member0_
         *         where
         *             member0_.age in (
         *                 select
         *                     member1_.age
         *                 from
         *                     member member1_
         *                 where
         *                     member1_.age>?
         *             )
         */
        //when

        //then
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * select 절에 subQuery
     */
    @Test
    void selectSubQuery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ).from(member)
                .fetch();

        /**
         * select
         *             member0_.username as col_0_0_,
         *             (select
         *                 avg(cast(member1_.age as double))
         *             from
         *                 member member1_) as col_1_0_
         *         from
         *             member member0_
         */

        //when
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple.get(member.username));
            System.out.println("tuple.get(JPAExpressions.select(memberSub.age.avg())) = " +
                    tuple.get(JPAExpressions.select(memberSub.age.avg())
                            .from(memberSub)));
        }


        //then
    }

    /**
     * Case 문
     * select, 조건절(where), order by에서 사용 가능
     * @throws Exception
     */
    @Test
    void basicCase() throws Exception {
        //given
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        /**
         * select
         *             case
         *                 when member0_.age=? then ?
         *                 when member0_.age=? then ?
         *                 else '기타'
         *             end as col_0_0_
         *         from
         *             member member0_
         */
        //when
        for (String s : result) {
            System.out.println("s = " + s);
        }

        //then
    }

    @Test
    void complexCase() throws Exception {
        //given
        List<String> result = queryFactory.select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        /**
         * select
         *             case
         *                 when member0_.age between ? and ? then ?
         *                 when member0_.age between ? and ? then ?
         *                 else '기타'
         *             end as col_0_0_
         *         from
         *             member member0_
         */

        //when
        for (String s : result) {
            System.out.println("s = " + s);
        }
        //then
    }

    /**
     * 다음과 같은 순서로 회원을 출력하고 싶다면?
     * 1. 0 ~ 30 살이 아닌 회원
     * 2. 0 ~ 20살 회원 출력
     * 3. 21 ~ 30살 회원 출력
     * @throws Exception
     */
    @Test
    void orderByWithCase() throws Exception {
        //given
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        /**
         * select
         *             member0_.username as col_0_0_,
         *             member0_.age as col_1_0_,
         *             case
         *                 when member0_.age between ? and ? then ?
         *                 when member0_.age between ? and ? then ?
         *                 else 3
         *             end as col_2_0_
         *         from
         *             member member0_
         *         order by
         *             case
         *                 when member0_.age between ? and ? then ?
         *                 when member0_.age between ? and ? then ?
         *                 else 3
         *             end desc
         */

        //when
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            System.out.println("username = " + username);

            Integer age = tuple.get(member.age);
            System.out.println("age = " + age);

            Integer rank = tuple.get(rankPath);
            System.out.println("rank = " + rank);
        }

        //then
    }

    /**
     * 상수
     * Expressions.constant("xxx") 사용
     */
    @Test
    void constant() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        /**
         * select
         *             member0_.username as col_0_0_
         *         from
         *             member member0_
         */

        //when
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 문자 더하기
     *
     * member.age.stringValue() 부분이 중요한데,
     * 문자가 아닌 다른 타입들은 stringValue() 로 문자로 변환할 수 있다.
     * 이 방법은 ENUM을 처리할 때도 자주 사용한다.
     */
    @Test
    void concat() throws Exception {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        /**
         * select
         *             ((member0_.username||?)||cast(member0_.age as character varying)) as col_0_0_
         *         from
         *             member member0_
         *         where
         *             member0_.username=?
         */

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void simpleProjection() throws Exception {
        //given
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
        //when

        //then
    }

    /**
     * 튜플 조회
     * 프로젝션 대상이 둘 이상일 때 사용
     */
    @Test
    void tupleProjection() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        /**
         * select
         *             member0_.username as col_0_0_,
         *             member0_.age as col_1_0_
         *         from
         *             member member0_
         */

        //when
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            System.out.println("username = " + username);
            Integer age = tuple.get(member.age);
            System.out.println("age = " + age);
        }

        //then
    }

    /**
     * 1. 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
     * 2. DTO의 package이름을 다 적어줘야해서 지저분함
     * 3. 생성자 방식만 지원함
     */
    @Test
    void findDtoByJpql() throws Exception {
        //given
        List<MemberDto> memberDtos = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();
        //when
        for (MemberDto memberDto : memberDtos) {
            System.out.println(memberDto);
        }
        //then
    }

    /**
     * Querydsl 빈 생성(Bean population)
     * 프로퍼티 접근 - Setter
     */
    @Test
    void findDtoBySetter() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //when
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

        //then
    }

    /**
     * Querydsl 빈 생성(Bean population)
     * 필드 직접 접근(필드명이 일치해야함)
     */
    @Test
    void findDtoByField() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //when
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

        //then
    }

    /**
     * Querydsl 빈 생성(Bean population)
     * 생성자 사용
     */
    @Test
    void findDtoByConstructor() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //when
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

        //then
    }

    /**
     * Querydsl 빈 생성(Bean population)
     * 별칭이 다를 때
     * 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
     * ExpressionUtils.as(source,alias) : 필드나, 서브 쿼리에 별칭 적용
     * username.as("memberName") : 필드에 별칭 적용
     */
    @Test
    void findUserDto() throws Exception {

        QMember memberSub = new QMember("memberSub");

        //given
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        //서브쿼리의 결과가 UserDto의 age에 매칭되어서 들어간다.
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
        //when

        //then
    }

}
