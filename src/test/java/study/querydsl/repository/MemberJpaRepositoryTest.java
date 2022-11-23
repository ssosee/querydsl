package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entitiy.Member;
import study.querydsl.entitiy.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {
    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @BeforeEach
    void before() {
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
    void basicTest() throws Exception {
        //given
        Member member = new Member("member5", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

//        List<Member> result1 = memberJpaRepository.findAll_Querydsl();
//        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJpaRepository.findByUsername_Querydsl("member5");
        //when

        //then
        assertThat(result2).containsExactly(member);
    }

    /**
     * 가급적이면 paging query도 같이 들어가야 한다.
     */
    @Test
    void searchTest() throws Exception {
        //given
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(20);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
        //when
        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        /**
         * select
         *             member0_.id as col_0_0_,
         *             member0_.username as col_1_0_,
         *             member0_.age as col_2_0_,
         *             team1_.id as col_3_0_,
         *             team1_.name as col_4_0_
         *         from
         *             member member0_
         *         left outer join
         *             team team1_
         *                 on member0_.team_id=team1_.id
         *         where
         *             team1_.name=?
         *             and member0_.age>=?
         *             and member0_.age<=?
         */
        //then
        assertThat(result).extracting("username").containsExactly("member3", "member4");
    }

    @Test
    void searchTest2() throws Exception {
        //given
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(20);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
        //when
        List<MemberTeamDto> result = memberJpaRepository.search(condition);
        /**
         * select
         *             member0_.id as col_0_0_,
         *             member0_.username as col_1_0_,
         *             member0_.age as col_2_0_,
         *             team1_.id as col_3_0_,
         *             team1_.name as col_4_0_
         *         from
         *             member member0_
         *         left outer join
         *             team team1_
         *                 on member0_.team_id=team1_.id
         *         where
         *             team1_.name=?
         *             and member0_.age>=?
         *             and member0_.age<=?
         */
        //then
        assertThat(result).extracting("username").containsExactly("member3", "member4");
    }

}