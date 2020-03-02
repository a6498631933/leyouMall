<template>


  <v-card>
    <!-- 卡片的头部 -->
    <v-card-title>
      <v-btn color="primary" @click="addBrand">新增品牌</v-btn>
      <!--空间隔离组件-->
      <v-spacer />
      <!--搜索框，与search属性关联-->
      <v-text-field label="输入关键字搜索" v-model="search" append-icon="search" hide-details/>
    </v-card-title>


    <!-- 分割线 -->
    <v-divider/>
    <!--卡片的中部-->
    <v-data-table
      :headers="headers"
      :items="brands"
      :search="search"
      :pagination.sync="pagination"
      :total-items="totalBrands"
      :loading="loading"
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <td>{{ props.item.id }}</td>
        <td class="text-xs-center">{{ props.item.name }}</td>
        <td class="text-xs-center"><img :src="props.item.image"></td>
        <td class="text-xs-center">{{ props.item.letter }}</td>
        <td class="justify-center layout">
          <v-btn color="info">编辑</v-btn>
          <v-btn color="warning">删除</v-btn>
        </td>
      </template>
    </v-data-table>

    <!--弹出的对话框-->
    <v-dialog max-width="500" v-model="show" persistent>
      <v-card>
        <!--对话框的标题-->
        <v-toolbar dense dark color="primary">
          <v-toolbar-title>新增品牌</v-toolbar-title>
          <v-spacer/>
          <!--关闭窗口的按钮-->
          <v-btn icon @click="closeWindow"><v-icon>close</v-icon></v-btn>
        </v-toolbar>
        <!--对话框的内容，表单-->
        <v-card-text class="px-5">
<!--          <my-brand-form :oldBrand="brand" :isEdit="isEdit" @close="show = false" :reload="getDataFromApi"/>-->
          <my-brand-form :oldBrand="brand" @close="closeWindow"/>
<!--          <my-brand-form :oldBrand="brand" :isEdit="isEdit" @close="show = false" :reload="getDataFromApi"/>-->
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<script>
  // 导入自定义的表单组件
  import MyBrandForm from './MyBrandForm'

  export default {
      data() {
        return {
          search: '', // 搜索过滤字段
          totalBrands: 0, // 总条数
          brands: [], // 当前页品牌数据
          loading: true, // 是否在加载中
          pagination: {}, // 分页信息
          headers: [ // 头信息
            {text: 'id', align: 'center', value: 'id'},
            {text: '名称', align: 'center', sortable: false, value: 'name'},
            {text: 'LOGO', align: 'center', sortable: false, value: 'image'},
            {text: '首字母', align: 'center', value: 'letter', sortable: true,},
            {text: '操作', align: 'center', value: 'id', sortable: false}
          ],
          show:false,
          brand: {}, // 品牌信息
          // isEdit: false // 判断是编辑还是新增
        }
      },
    methods:{
      addBrand() {
        // this.brand = {};
        // this.isEdit = false;
        this.show = true;
      },
      closeWindow(){
        // 关闭窗口
        this.show = false;
        // 重新加载数据
        this.getDataFromServer();
      },
      getDataFromServer(){ // 从服务的加载数据的方法。
        // 发起请求
        this.$http.get("/item/brand/page", {
          params: {
            key: this.search, // 搜索条件
            page: this.pagination.page,// 当前页
            rows: this.pagination.rowsPerPage,// 每页大小
            sortBy: this.pagination.sortBy,// 排序字段
            desc: this.pagination.descending// 是否降序
          }
        }).then(resp => { // 这里使用箭头函数
          this.brands = resp.data.list;
          this.totalBrands = resp.data.total;
          // 完成赋值后，把加载状态赋值为false
          this.loading = false;
        })

        // // 伪造假数据
        // const brands = [
        //   {
        //     "id": 2032,
        //     "name": "OPPO",
        //     "image": "http://img10.360buyimg.com/popshop/jfs/t2119/133/2264148064/4303/b8ab3755/56b2f385N8e4eb051.jpg",
        //     "letter": "O",
        //     "categories": null
        //   },
        //   {
        //     "id": 2033,
        //     "name": "飞利浦（PHILIPS）",
        //     "image": "http://img12.360buyimg.com/popshop/jfs/t18361/122/1318410299/1870/36fe70c9/5ac43a4dNa44a0ce0.jpg",
        //     "letter": "F",
        //     "categories": null
        //   },
        //   {
        //     "id": 2034,
        //     "name": "华为（HUAWEI）",
        //     "image": "http://img10.360buyimg.com/popshop/jfs/t5662/36/8888655583/7806/1c629c01/598033b4Nd6055897.jpg",
        //     "letter": "H",
        //     "categories": null
        //   },
        //   {
        //     "id": 2036,
        //     "name": "酷派（Coolpad）",
        //     "image": "http://img10.360buyimg.com/popshop/jfs/t2521/347/883897149/3732/91c917ec/5670cf96Ncffa2ae6.jpg",
        //     "letter": "K",
        //     "categories": null
        //   },
        //   {
        //     "id": 2037,
        //     "name": "魅族（MEIZU）",
        //     "image": "http://img13.360buyimg.com/popshop/jfs/t3511/131/31887105/4943/48f83fa9/57fdf4b8N6e95624d.jpg",
        //     "letter": "M",
        //     "categories": null
        //   }
        // ];
        // // 模拟延迟一段时间，随后进行赋值
        // setTimeout(() => {
        //   // 然后赋值给brands
        //   this.brands = brands;
        //   this.totalBrands = brands.length;
        //   // 完成赋值后，把加载状态赋值为false
        //   this.loading = false;
        // },400)
      }
    },
    mounted(){ // 渲染后执行
      // 查询数据
      this.getDataFromServer();
    },
    components:{
      MyBrandForm
    }
  }
</script>

<style scoped>

</style>
