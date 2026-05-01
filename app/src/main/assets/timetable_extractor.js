(() => {
	try {
		const xhrTerm = new XMLHttpRequest();
		xhrTerm.open("GET", "/jwapp/sys/wdkb/modules/jshkcb/xnxqcx.do", false);
		xhrTerm.send();
		const termRes = JSON.parse(xhrTerm.responseText);
		const terms = termRes.datas.xnxqcx.rows;
		terms.sort((a, b) => parseInt(b.PX, 10) - parseInt(a.PX, 10));
		let targetTerm = terms[0];

		const activeText = document.body.innerText;
		for (let i = 0; i < terms.length; i++) {
			if (activeText.indexOf(terms[i].MC) !== -1) {
				targetTerm = terms[i];
				break;
			}
		}

		console.log(`最终命中探测: ${targetTerm.MC}`);

		const xhrKb = new XMLHttpRequest();

		xhrKb.open(
			"GET",
			`/jwapp/sys/wdkb/modules/xskcb/xskcb.do?XNXQDM=${targetTerm.DM}`,
			false,
		);

		xhrKb.send();
		const kbData = JSON.parse(xhrKb.responseText);

		// 💡 检查数据有效性

		if (
			!kbData ||
			!kbData.datas ||
			!kbData.datas.xskcb ||
			kbData.datas.xskcb.totalCount === 0
		) {
			throw new Error("当前学期未查询到课程数据，请确认是否已登录或选课。");
		}

		if (window.AndroidBridge) {
			window.AndroidBridge.sendData(
				JSON.stringify({
					termName: targetTerm.MC,

					rows: kbData.datas.xskcb.rows,
				}),
			);
		}
	} catch (e) {
		if (window.AndroidBridge) {
			window.AndroidBridge.onError(e.message || "未知错误，请重试");
		}
	}
})();
