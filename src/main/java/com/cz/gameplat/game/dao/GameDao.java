package com.cz.gameplat.game.dao;

import com.cz.framework.dao.*;
import com.cz.gameplat.game.entity.*;
import org.springframework.stereotype.*;
import java.io.*;
import org.springframework.cache.annotation.*;
import java.util.*;

@Repository
public class GameDao extends MysqlBaseDaoImpl<Game, Integer>
{
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #gameId"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public int updateGameRestDate(final Integer gameId, final Date startDate, final Date endDate) {
        return this.buildSQL("UPDATE game SET  rest_start_date = ?,rest_end_date=? WHERE id=? ").addArgs(new Object[] { startDate, endDate, gameId }).execSQL();
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #gameId"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public int updateGameCurTurnNum(final Integer gameId, final String curTurnNum) {
        return this.buildSQL("UPDATE game SET  cur_turn_num = ? WHERE id=? ").addArgs(new Object[] { curTurnNum, gameId }).execSQL();
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_all" }, key = "'all'") }, put = { @CachePut(value = { "game_info" }, key = "'id_' + #gameId") })
    public Game updateGameCurTurnNumByAdditive(final Integer gameId) {
        this.buildSQL("UPDATE game SET  cur_turn_num = cur_turn_num+amount WHERE id=? ").addArgs(new Object[] { gameId }).execSQL();
        return this.get(gameId);
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #gameId"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public int updateGameIsBan(final Integer gameId, final Integer isBan) {
        return this.buildSQL("UPDATE game SET  is_ban = ? WHERE id=? ").addArgs(new Object[] { isBan, gameId }).execSQL();
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #gameId"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public int updateGameOpen(final Integer gameId, final Integer open) {
        return this.buildSQL("UPDATE game SET  open = ? WHERE id=? ").addArgs(new Object[] { open, gameId }).execSQL();
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #gameId"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public int updateRestEndDate(final Integer gameId, final Date RestEndDate) {
        return this.buildSQL("UPDATE game SET  rest_end_date = ? WHERE id=? ").addArgs(new Object[] { RestEndDate, gameId }).execSQL();
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #gameId"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public int updateRestStartDate(final Integer gameId, final Date RestStartDate) {
        return this.buildSQL("UPDATE game SET  rest_start_date = ? WHERE id=? ").addArgs(new Object[] { RestStartDate, gameId }).execSQL();
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #gameId"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public int updateCurTurnNum(final Integer gameId, final String curTurnNum) {
        return this.buildSQL("UPDATE game SET  cur_turn_num = ? WHERE id=? ").addArgs(new Object[] { curTurnNum, gameId }).execSQL();
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #gameId"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public int updateSort(final Integer gameId, final Integer sort) {
        return this.buildSQL("UPDATE game SET  sort = ? WHERE id=? ").addArgs(new Object[] { sort, gameId }).execSQL();
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #game.id"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public int update(final Game game) {
        return this.buildSQL("UPDATE game SET ").addSqlAndArgs("id=?", game.getId()).addSqlAndArgs(",name=?", game.getName()).addSqlAndArgs(",sort=?", game.getSort()).addSqlAndArgs(",cate=?", game.getCate()).addSqlAndArgs(",max_reward=?", game.getMaxReward()).addSqlAndArgs(",open=?", game.getOpen()).addSqlAndArgs(",rest_start_date=?,rest_end_date=?", new Object[] { game.getRestStartDate(), game.getRestEndDate() }).addSqlAndArgs(",turn_format=?", game.getTurnFormat()).addSqlAndArgs(",cur_turn_num=?", game.getCurTurnNum()).addSqlAndArgs(",is_ban=?", game.getIsBan()).addSqlAndArgs(" WHERE id=? ", game.getId()).execSQL();
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #game.id"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public int updateCollectType(final Game game) {
        return this.buildSQL("UPDATE game SET collect_type=? ").addArg(game.getCollectType()).addSqlAndArgs(" WHERE id=? ", game.getId()).execSQL();
    }
    
    @Cacheable(value = { "game_info" }, key = "'id_' + #id")
    public Game get(final Integer id) {
        return (Game)super.get(id);
    }
    
    @Cacheable(value = { "game_all" }, key = "'all'")
    public List<Game> queryAll() {
        return (List<Game>)this.buildSelect().addSql(" ORDER BY `sort` ").queryList();
    }
    
    public List<Game> queryAll(final Integer openStatus) {
        return (List<Game>)this.buildSelect().addSqlAndArgs(" where open=?", openStatus).addSql(" ORDER BY `sort` ").queryList();
    }
    
    public List<Game> getGameByCate(final Integer cate) {
        return (List<Game>)this.buildSelect().addSqlAndArgs(" where cate=?", cate).queryList();
    }
    
    @Caching(evict = { @CacheEvict(value = { "game_info" }, key = "'id_' + #id"), @CacheEvict(value = { "game_all" }, key = "'all'") })
    public void updateName(final Integer id, final String name) {
        this.buildSQL("UPDATE game SET  name = ? WHERE id=? ").addArgs(new Object[] { name.trim(), id }).execSQL();
    }
}
