let items = [];
let files = [];

document.getElementById("invoice-form").addEventListener("submit", function (e) {
    e.preventDefault();
    const itemName = document.getElementById("itemName").value;
    const quantity = parseInt(document.getElementById("quantity").value);
    const amount = parseFloat(document.getElementById("amount").value);

    if (!itemName || isNaN(quantity) || isNaN(amount)) return;

    items.push({ itemName, quantity, amount });
    renderTable();

    document.getElementById("itemName").value = '';
    document.getElementById("quantity").value = '';
    document.getElementById("amount").value = '';
});

function renderTable() {
    const tbody = document.querySelector("#items-table tbody");
    tbody.innerHTML = "";
    items.forEach((item, index) => {
        const total = item.quantity * item.amount;
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${item.itemName}</td>
            <td>${item.quantity}</td>
            <td>$${item.amount.toFixed(2)}</td>
            <td>$${total.toFixed(2)}</td>
            <td><button onclick="removeItem(${index})">Remove</button></td>
        `;
        tbody.appendChild(row);
    });
}

function removeItem(index) {
    items.splice(index, 1);
    renderTable();
}

document.getElementById("fileInput").addEventListener("change", function () {
    files = [...files, ...Array.from(this.files)];
    renderFileList();
    this.value = "";
});

function renderFileList() {
    const list = document.getElementById("file-list");
    list.innerHTML = "";
    files.forEach((file, index) => {
        const li = document.createElement("li");
        li.textContent = file.name;
        const btn = document.createElement("button");
        btn.textContent = "Remove";
        btn.onclick = () => {
            files.splice(index, 1);
            renderFileList();
        };
        li.appendChild(btn);
        list.appendChild(li);
    });
}

function generateInvoice(endpoint) {
    const location = document.getElementById("location").value;
    if (!location || (items.length === 0 && files.length === 0)) {
        alert("Please enter a location and add at least one item or file.");
        return;
    }

    const formData = new FormData();
    formData.append("location", location);
    formData.append("items", new Blob([JSON.stringify(items)], { type: "application/json" }));
    files.forEach(file => formData.append("files", file));

    fetch(endpoint, {
        method: "POST",
        body: formData
    })
        .then(response => {
            if (!response.ok) throw new Error("Invoice generation failed");
            return response.blob();
        })
        .then(blob => {
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "invoice.pdf";
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
            document.getElementById("result").innerHTML = "<p>Invoice downloaded!</p>";
        })
        .catch(error => {
            console.error(error);
            document.getElementById("result").innerHTML = "<p>Error generating invoice.</p>";
        });
}


document.getElementById("generate-btn-tess").addEventListener("click", function () {
    generateInvoice("http://localhost:8080/api/invoice/generate");
});

document.getElementById("generate-btn-gcp").addEventListener("click", function () {
    generateInvoice("http://localhost:8080/api/invoice/generate-google");
});