let items = [];
let files = [];

// Handle item addition
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

// Handle file input
document.getElementById("fileInput").addEventListener("change", function () {
    const selectedFiles = Array.from(this.files);
    files = [...files, ...selectedFiles];
    renderFileList();
    this.value = ""; // allow selecting same file again
});

function renderFileList() {
    const list = document.getElementById("file-list");
    list.innerHTML = "";
    files.forEach((file, index) => {
        const li = document.createElement("li");
        li.textContent = file.name;
        const removeBtn = document.createElement("button");
        removeBtn.textContent = "Remove";
        removeBtn.onclick = () => {
            files.splice(index, 1);
            renderFileList();
        };
        li.appendChild(removeBtn);
        list.appendChild(li);
    });
}

// Handle invoice generation
document.getElementById("generate-btn").addEventListener("click", function () {
    const location = document.getElementById("location").value;
    if (!location || (items.length === 0 && files.length===0)) {
        alert("Please fill location and add at least one item.");
        return;
    }

    const formData = new FormData();
    formData.append("location", location);
    formData.append("items", new Blob([JSON.stringify(items)], { type: "application/json" }));

    files.forEach(file => {
        formData.append("files", file);
    });

    fetch("http://localhost:8080/api/invoice/generate", {
        method: "POST",
        body: formData
    })
        .then(response => {
            if (!response.ok) throw new Error("Failed to generate invoice");
            return response.blob();
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "invoice.pdf";
            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);
            document.getElementById("result").innerHTML = `<p>Invoice downloaded!</p>`;
        })
        .catch(error => {
            console.error("Error:", error);
            document.getElementById("result").innerHTML = `<p>Something went wrong.</p>`;
        });
});
