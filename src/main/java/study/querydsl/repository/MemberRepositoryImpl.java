package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import java.util.List;

import static study.querydsl.entitiy.QMember.member;
import static study.querydsl.entitiy.QTeam.team;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    /**
     * 스프링 데이터 페이징 활용1 - Querydsl 페이징 연동
     * <p>
     * 스프링 데이터의 Page, Pageable을 활용해보자.
     * 전체 카운트를 한번에 조회하는 단순한 방법
     * 데이터 내용과 전체 카운트를 별도로 조회하는 방법
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {

        QueryResults<MemberTeamDto> result = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = result.getResults();
        long total = result.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

//        long count = queryFactory
//                .select(member.count())
//                .from(member)
////                .leftJoin(member.team, team)
//// 전체 카운트를 조회할 때 조인 쿼리를 줄일 수 있다면 상당한 효과가 있다.
//                .where(
//                        usernameEq(condition.getUsername()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
//                )
//                .fetchCount();

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        /**
         * count 쿼리가 생략 가능한 경우 생략해서 처리
         * PageableExecutionUtils를 사용하면 count 쿼리 최적화 가능
         * 내부적으로 count가 필요없으면 조회해오지 않는다.
         * 그 이유는 람다를 사용하여 게으른 호출로 인하여 실제 사용되기 전까지는 함수가 실행되지 않는다.
         *
         * 다음 2가지 경우에 count 쿼리가 실행되지 않는다.
         * 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈 보다 작을때
         * e.g)
         *  페이지 시작(0)이면서 컨텐츠 사이즈(3)가 페이지 사이즈(10)보다 작다면 컨텐츠 사이즈(3)가 카운트 갯수
         *
         * 마지막 페이지 일때(offset + 컨텐츠 사이즈 = 카운트)
         * e.g)
         *  예를 들어 전체 데이터는 103개이고, 페이지 사이즈는 10이라고 한다면
         *  마지막 페이지 일때는 offset(100), 컨텐츠 사이즈(3)이라서 카운트는 103으로 이해 하고 있는데 맞는 내용 인가요?
         */
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
        
        //return new PageImpl<>(content, pageable, count);
    }

    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
