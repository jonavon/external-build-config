<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8" />
	<title>${htmlTitle}</title>
	<style type="text/css">
	body {
		font: normal medium/1.4 sans-serif;
	}
	table {
		border-collapse: collapse;
		width: 100%;
	}
	th, td {
		padding: 0.25rem;
		text-align: left;
		border: 1px solid #ccc;
	}
	tbody tr:nth-child(odd) {
		background: #eee;
	}
	</style>
</head>
<body>
	<h1>${htmlTitle}</h1>
	<g:if test="${flash.message}">
		<div class="message" role="status">${flash.message}</div>
	</g:if>
	<table>
		<thead>
			<tr>
				<td>Key</td>
				<td>Value</td>
			</tr>
		</thead>
		<tbody>
			<g:each in="${buildSettings}" var="setting">
			<tr>
				<td>${setting.key}</td>
				<td>${setting.value}</td>
			</tr>
			</g:each>
		</tbody>
	</table>
</body>
</html>
