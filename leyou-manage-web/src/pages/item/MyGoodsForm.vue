<template>
  <v-stepper v-model="step">
    <v-stepper-header>
      <v-stepper-step :complete="step > 1" step="1">商品描述</v-stepper-step>
      <v-divider/>
      <v-stepper-step :complete="step > 2" step="2">商品描述</v-stepper-step>
      <v-divider/>
      <v-stepper-step :complete="step > 3" step="3">规格参数</v-stepper-step>
      <v-divider/>
      <v-stepper-step step="4">SKU属性</v-stepper-step>
    </v-stepper-header>
    <v-stepper-items>
      <v-stepper-content step="1">
        基本信息
        <v-cascader
          url="/item/category/list"
          required
          showAllLevels
          v-model="goods.categories"
          label="请选择商品分类"/>

        品牌
        <v-select
          :items="brandOptions"
          item-text="name"
          item-value="id"
          label="所属品牌"
          v-model="goods.brandId"
          required
          autocomplete
          clearable
          dense chips
        />

        <v-text-field label="商品标题" v-model="goods.title" :counter="200" required />
        <v-text-field label="商品卖点" v-model="goods.subTitle" :counter="200"/>
        <v-text-field label="包装清单" v-model="goods.spuDetail.packingList" :counter="1000" multi-line :rows="3"/>
        <v-text-field label="售后服务" v-model="goods.spuDetail.afterService" :counter="1000" multi-line :rows="3"/>

      </v-stepper-content>
      <v-stepper-content step="2">
        规格参数
        <v-flex class="xs10 mx-auto px-3">
          <!--遍历整个规格参数，获取每一组-->
          <v-card v-for="spec in specifications" :key="spec.group" class="my-2">
            <!--组名称-->
            <v-card-title class="subheading">{{spec.group}}</v-card-title>
            <!--遍历组中的每个属性，并判断是否是全局属性，不是则不显示-->
            <v-card-text v-for="param in spec.params" :key="param.k" v-if="param.global" class="px-5">
              <!--判断是否有可选项，如果没有，则显示文本框。还要判断是否是数值类型，如果是把unit显示到后缀-->
              <v-text-field v-if="param.options.length <= 0"
                            :label="param.k" v-model="param.v" :suffix="param.unit || ''"/>
              <!--否则，显示下拉选项-->
              <v-select v-else :label="param.k" v-model="param.v" :items="param.options"/>
            </v-card-text>
          </v-card>
        </v-flex>
      </v-stepper-content>
      <v-stepper-content step="3">
        商品描述
        <quill-editor v-model="goods.spuDetail.description" :options="editorOption" upload-url="/upload/image"/>

      </v-stepper-content>
      <v-stepper-content step="4">
        <!--4、SKU属性-->
        <v-flex class="mx-auto">
          <!--遍历特有规格参数-->
          <v-card flat v-for="spec in specialSpecs" :key="spec.k">
            <!--特有参数的标题-->
            <v-card-title class="subheading">{{spec.k}}:</v-card-title>
            <!--特有参数的待选项，需要判断是否有options，如果没有，展示文本框，让用户自己输入-->
            <v-card-text v-if="spec.options.length <= 0" class="px-5">
              <div v-for="i in spec.selected.length+1" :key="i" class="layout row">
                <v-text-field :label="'输入新的' + spec.k" class="flex xs8" v-model="spec.selected[i-1]" v-bind:value="i"/>
                <v-spacer/>
                <v-btn small @click="spec.selected.splice(i-1,1)">删除</v-btn>
              </div>
            </v-card-text>
            <!--如果有options，需要展示成多个checkbox-->
            <v-card-text v-else class="container fluid grid-list-xs">
              <v-layout row wrap class="px-5">
                <v-checkbox color="primary" v-for="o in spec.options" :key="o" class="flex xs3"
                            :label="o" v-model="spec.selected" :value="o"/>
              </v-layout>
            </v-card-text>
          </v-card>
        </v-flex>
        <v-card>
          <!--标题-->
          <v-card-title class="subheading">SKU列表</v-card-title>
          <!--SKU表格，hide-actions因此分页等工具条-->
          <v-data-table :items="skus" :headers="headers" hide-actions item-key="indexes">
            <template slot="items" slot-scope="props">
              <tr @click="props.expanded = !props.expanded">
                <!--价格和库存展示为文本框-->
                <td v-for="(v,k) in props.item" :key="k" v-if="['price', 'stock'].includes(k)"
                    class="text-xs-center">
                  <v-text-field single-line v-model.number="props.item[k]"/>
                </td>
                <!--enable展示为checkbox-->
                <td class="text-xs-center" v-else-if="k === 'enable'">
                  <v-checkbox v-model="props.item[k]"/>
                </td>
                <!--indexes和images不展示，其它展示为普通文本-->
                <td class="text-xs-center" v-else-if="!['indexes','images'].includes(k)">{{v}}</td>
              </tr>
            </template>
            <!--点击表格后展开-->
            <template slot="expand" slot-scope="props">
              <v-card flat>
                <v-card-text class="title">商品图片</v-card-text>
<!--                <v-upload-->
<!--                  v-model="brand.image" url="/item/upload" :multiple="false" :pic-width="250" :pic-height="90"-->
<!--                />-->
              </v-card>
            </template>
          </v-data-table>
        </v-card>
        <!--提交按钮-->
        <v-flex xs3 offset-xs9>
          <v-btn color="info" @click="submit">保存商品信息</v-btn>
        </v-flex>
      </v-stepper-content>
    </v-stepper-items>
  </v-stepper>
</template>

<script>

  import 'quill/dist/quill.core.css'
  import 'quill/dist/quill.snow.css'
  import 'quill/dist/quill.bubble.css'

  import {quillEditor} from 'vue-quill-editor'

  export default {
    name: "goods-form",
    data() {
      return {
        editorOption: {// 富文本编辑器配置
          placeholder: '编写商品描述信息，让客户了解你的商品'
        },
        goods:{
          categories:[], // 商品3级分类数组信息
          brandId: 0,// 品牌id信息
          title: '',// 标题
          subTitle: '',// 子标题
          spuDetail: {
            packingList: '',// 包装列表
            afterService: '',// 售后服务
            description: '',
          },
        },
        brandOptions: [], //品牌参数的模版
        specifications: [],//规格参数列表
        specialSpecs:[],
      }
    },
    computed: {
      skus() {
        // 过滤掉用户没有填写数据的规格参数
        const arr = this.specialSpecs.filter(s => s.selected.length > 0);
        // 通过reduce进行累加笛卡尔积
        return  arr.reduce((last, spec, index) => {
          const result = [];
          last.forEach(o => {
            for(let i = 0; i < spec.selected.length; i++){
              const option = spec.selected[i];
              const obj = {};
              Object.assign(obj, o);
              obj[spec.k] = option;
              // 拼接当前这个特有属性的索引
              obj.indexes = (o.indexes||'') + '_'+ i
              if(index === arr.length - 1){
                // 如果发现是最后一组，则添加价格、库存等字段
                Object.assign(obj, { price:0, stock:0,enable:false, images:[]})
                // 去掉索引字符串开头的下划线
                obj.indexes = obj.indexes.substring(1);
              }
              result.push(obj);
            }
          })
          return result
        },[{}])
      },
      headers() {
        if(this.skus.length <= 0) {
          return [];
        }
        const headers = [];

        Object.keys(this.skus[0]).forEach( k=> {
          let value = k;
          if(k === 'price'){
            // enable，表头要翻译成“价格”
            k = '价格'
          }else if(k === 'stock'){
            // enable，表头要翻译成“库存”
            k = '库存';
          }else if(k === 'enable'){
            // enable，表头要翻译成“是否启用”
            k = '是否启用'
          } else if(k === 'indexes' || k === 'images'){
            // 图片和索引不在表格中展示
            return;
          }
          headers.push({
            text: k,
            align: 'center',
            sortable: false,
            value
          })
        })
        return headers;
      }
    },
    watch: {
      oldGoods: {
        deep: true,
        async handler(val) {
              if (val == null || !this.isEdit) {
                return;
              }
          // 实现数据回显
          // this.goods = Object.deepCopy(val)

          this.goods.brandId = val.brandId;
          this.goods.title = val.title;
          this.goods.subTitle = val.subTitle;
          this.goods.spuDetail.packingList = Object.deepCopy(val.spuDetail.packingList);
          this.goods.spuDetail.afterService = Object.deepCopy(val.spuDetail.afterService);
          this.goods.spuDetail.description = Object.deepCopy(val.spuDetail.description);
          // this.specifications = JSON.parse(val.spuDetail.specifications);
          // const temp = [];
          // this.specifications.forEach(({params}) => {
          //   params.forEach(({k, options, global}) => {
          //     if (!global) {
          //       temp.push({
          //         k, options, selected:[]
          //       })
          //     }
          //   })
          // })
          // this.specialSpecs = temp;

          let tempCategories = [];
          let categoryNames = val.cname.split('/');
          let cids = [val.cid1, val.cid2, val.cid3];
          categoryNames.forEach(function (name, index, array) {
            let category = {
              id: 0,
              name: ""
            };
            category.id =  cids[index];
            category.name = name;
            tempCategories.push(category);
          });
          this.goods.categories = tempCategories;
        }
      },
      'goods.categories': {
        deep: true,
        handler(val) {
          // 判断商品分类是否存在，存在才查询
          if (val && val.length > 0) {
            // 根据分类查询品牌
            this.$http.get("/item/brand/cid/" + this.goods.categories[2].id)
              .then(({data}) => {
                this.brandOptions = data;
              })

            this.$http.get("/item/spec/" + this.goods.categories[2].id)
              .then(({data}) => {

                if(this.isEdit){
                  //对特有规格进行筛选
                  let oldSpecifications = JSON.parse(this.oldGoods.spuDetail.specifications);
                  data.forEach(({params}, index) => {
                    let oldParams = oldSpecifications[index].params;
                    params.forEach((param, idx) => {
                      param.v = oldParams[idx].v;
                    })
                  })
                }


              const temp = [];
              data.forEach(({params}) => {
                params.forEach(({k, options, global}) => {
                  if (!global) {
                    temp.push({
                      k, options, selected:[]
                    })
                  }
                })
              })

                if(this.isEdit){
                  //对特有规格进行筛选
                  let oldSpecialSpecs = JSON.parse(this.oldGoods.spuDetail.specTemplate);
                  console.log("************" + oldSpecialSpecs);
                  temp.forEach((tempSpec, index) => {
                    console.log("************" + tempSpec);
                    console.log("************222" + tempSpec.k);
                    console.log("************333" + oldSpecialSpecs["机身内存"]);
                    tempSpec.selected = oldSpecialSpecs[tempSpec.k];
                  })
                }

              this.specialSpecs = temp;

              //保存全部规格
              this.specifications = data;
              })
          }
        }
      }
    },
    props: {
      step: {
        type:Number,
        default: 1
      },
      isEdit: {
        type: Boolean,
        default: false
      },
      oldGoods: Object
    },
    components: {
      quillEditor
    },
    methods: {
      submit(){
        // 表单校验。 略
        // 先处理goods，用结构表达式接收,除了categories外，都接收到goodsParams中
        const {categories: [{id:cid1},{id:cid2},{id:cid3}], ...goodsParams} = this.goods;
        // 处理规格参数
        const specs = this.specifications.map(({group,params}) => {
          const newParams = params.map(({options,...rest}) => {
            return rest;
          })
          return {group,params:newParams};
        });
        // 处理特有规格参数模板
        const specTemplate = {};
        this.specialSpecs.forEach(({k, selected}) => {
          specTemplate[k] = selected;
        });
        // 处理sku
        const skus = this.skus.filter(s => s.enable).map(({price,stock,enable,images,indexes, ...rest}) => {
          // 标题，在spu的title基础上，拼接特有规格属性值
          const title = goodsParams.title + " " + Object.values(rest).join(" ");
          return {
            price: this.$format(price+""),stock,enable,indexes,title,// 基本属性
            images: !images ? '' : images.join(","), // 图片
            ownSpec: JSON.stringify(rest), // 特有规格参数
          }
        });
        Object.assign(goodsParams, {
          cid1,cid2,cid3, // 商品分类
          skus, // sku列表
        })
        goodsParams.spuDetail.specifications= JSON.stringify(specs);
        goodsParams.spuDetail.specTemplate = JSON.stringify(specTemplate);

        console.log(goodsParams)

        if(this.isEdit){
          this.$http.put("/item/goods",goodsParams)
            .then(() => {
              // 成功，关闭窗口
              this.$emit('close');
              // 提示成功
              this.$message.success("保存成功")
            })
            .catch(() => {
              this.$message.error("保存失败！");
            });
        }
        else {
          this.$http.post("/item/goods",goodsParams)
            .then(() => {
              // 成功，关闭窗口
              this.$emit('close');
              // 提示成功
              this.$message.success("新增成功了")
            })
            .catch(() => {
              this.$message.error("保存失败！");
            });
        }



      }
    }
  }
</script>

<style scoped>

</style>
