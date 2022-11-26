package study.querydsl.repository.support;

import com.mysema.commons.lang.Assert;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;

/**
 * Querydsl 지원 클래스 직접 만들기
 * 스프링 데이터가 제공하는 QuerydslRepositorySupport 가 지닌 한계를 극복하기 위해 직접 Querydsl
 * 지원 클래스를 만들어보자.
 *
 * 스프링 데이터가 제공하는 페이징을 편리하게 변환 페이징과 카운트 쿼리 분리 가능
 * 스프링 데이터 Sort 지원
 * select() , selectFrom() 으로 시작 가능 EntityManager, QueryFactory 제공
 */

/**
 * Querydsl 4.x 버전에 맞춘 Querydsl 지원 라이브러리
 *
 * @author Seaung Jang
 * @see org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
 */
@Repository
public abstract class Querydsl4RepositorySupport {
    private final Class domainClass;
    private Querydsl querydsl;
    private EntityManager entityManager;
    private JPAQueryFactory queryFactory;

    public Querydsl4RepositorySupport(Class<?> domainClass) {
        Assert.notNull(domainClass, "Domain class must not be null");
        this.domainClass = domainClass;
    }

    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        Assert.notNull(entityManager, "EntityManager must not be null");
    }
}
