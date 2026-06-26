package org.edu_sharing.service.contributor.ibatis;

import org.apache.ibatis.annotations.*;
import org.edu_sharing.service.contributor.ContributorEntry;
import org.edu_sharing.service.search.SearchService;

import java.util.List;

@Mapper
public interface ContributorMapper {

    String COLUMNS = "id, kind, title, givenname, surname, org, email, url, uid, orcid, gnduri, ror, wikidata, vcard, created, last_updated";

    @Insert("INSERT INTO edu_contributor(kind, title, givenname, surname, org, email, url, uid, orcid, gnduri, ror, wikidata, vcard, created, last_updated) " +
            "VALUES (#{kind}, #{title}, #{givenname}, #{surname}, #{org}, #{email}, #{url}, #{uid}, #{orcid}, #{gnduri}, #{ror}, #{wikidata}, #{vcard}, #{created}, #{lastUpdated})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void create(ContributorEntry entry);

    @Update("UPDATE edu_contributor SET " +
            "kind = #{kind}, title = #{title}, givenname = #{givenname}, surname = #{surname}, org = #{org}, " +
            "email = #{email}, url = #{url}, uid = #{uid}, orcid = #{orcid}, gnduri = #{gnduri}, ror = #{ror}, " +
            "wikidata = #{wikidata}, vcard = #{vcard}, last_updated = #{lastUpdated} " +
            "WHERE id = #{id}")
    void update(ContributorEntry entry);

    @Delete("DELETE FROM edu_contributor WHERE id = #{id}")
    void delete(long id);

    @Select("SELECT " + COLUMNS + " FROM edu_contributor WHERE id = #{id}")
    @Results(id = "contributorResult", value = {
            @Result(column = "last_updated", property = "lastUpdated")
    })
    ContributorEntry getById(long id);

    @Select("SELECT " + COLUMNS + " FROM edu_contributor ORDER BY surname, org, givenname LIMIT #{limit} OFFSET #{skip}")
    @ResultMap("contributorResult")
    List<ContributorEntry> getAll(long skip, int limit);

    @Select("SELECT COUNT(*) FROM edu_contributor")
    long count();

    /**
     * Autocomplete search: matches the search word against name/org/email columns, optionally filtered by kind.
     */
    @Select("<script>" +
            "SELECT " + COLUMNS + " FROM edu_contributor " +
            "<where> " +
            "  <if test='kind != null'> AND kind = #{kind} </if>" +
            "  <if test='searchWord != null and searchWord != \"\"'> AND (" +
            "      givenname ILIKE '%' || #{searchWord} || '%' " +
            "   OR surname ILIKE '%' || #{searchWord} || '%' " +
            "   OR org ILIKE '%' || #{searchWord} || '%' " +
            "   OR email ILIKE '%' || #{searchWord} || '%' " +
            "  ) </if>" +
            "</where> " +
            "ORDER BY surname, org, givenname LIMIT #{limit}" +
            "</script>")
    @ResultMap("contributorResult")
    List<ContributorEntry> search(@Param("searchWord") String searchWord,
                                  @Param("kind") SearchService.ContributorKind kind,
                                  @Param("limit") int limit);

    /**
     * Lookup by any of the persistent ids - used for deduplication during migration and for create/update validation.
     * Only non-null arguments are considered.
     */
    @Select("<script>" +
            "SELECT " + COLUMNS + " FROM edu_contributor " +
            "<where>" +
            "  <if test='orcid != null'> OR orcid = #{orcid} </if>" +
            "  <if test='gnduri != null'> OR gnduri = #{gnduri} </if>" +
            "  <if test='ror != null'> OR ror = #{ror} </if>" +
            "  <if test='wikidata != null'> OR wikidata = #{wikidata} </if>" +
            "  <if test='email != null'> OR email = #{email} </if>" +
            "</where>" +
            "</script>")
    @ResultMap("contributorResult")
    List<ContributorEntry> findByAnyId(@Param("orcid") String orcid,
                                       @Param("gnduri") String gnduri,
                                       @Param("ror") String ror,
                                       @Param("wikidata") String wikidata,
                                       @Param("email") String email);
}
