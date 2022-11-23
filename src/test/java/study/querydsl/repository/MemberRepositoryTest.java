package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entitiy.Member;
import study.querydsl.entitiy.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;
    @Autowired
    MemberRepository memberRepository;

    @Test
    void basicTest() throws Exception {
        //given
        Member member = new Member("member5", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberRepository.findByUsername("member5");
        //when

        //then
        assertThat(result2).containsExactly(member);
    }

    @Test
    void searchTest3() throws Exception {
        //given
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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(20);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
        //when
        List<MemberTeamDto> result = memberRepository.search(condition);
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
    void searchPageSimple() throws Exception {
        //given
        //given
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

        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);

        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);
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
         *                 on member0_.team_id=team1_.id limit ?
         */

        /**
         * select
         *             count(member0_.id) as col_0_0_
         *         from
         *             member member0_
         */
        //when

        //then
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }
}