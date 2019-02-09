<table class="Hyzx-table mt30">
	<tr onclick="Hyzx_tr_can_see(${game.id})" style="cursor:pointer;">
		<th style="width:208px">${game.name}</th>
		<th style="width:156px">退水设定</th>
		<th style="width:200px">单场限额</th>
		<th style="width:178px">单注限额</th>
		<th style="width:157px">最低限额</th>
	</tr>
	<#list playCateList as cate>
		<tr class="Hyzx_not_fist_tr_${game.id}" style="display:none">
			<td>${cate.name?default("")}</td>
			<td>${cate.rebate?default("")}</td>
			<td>${cate.maxTurnMoney?default("")}</td>
			<td>${cate.maxMoney?default("")}</td>
			<td>${cate.minMoney?default("")}</td>
		</tr>
	</#list>
</table>